package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.command.JarCommand;
import com.github.sormuras.bach.command.JavacCommand;
import com.github.sormuras.bach.internal.PathSupport;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.FolderType;
import com.github.sormuras.bach.project.ProjectSpace;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Compiles and archives Java source files. */
public class CompileWorkflow extends AbstractSpaceWorkflow {

  public CompileWorkflow(Bach bach, Project project, ProjectSpace space) {
    super(bach, project, space);
  }

  protected JavacCommand computeJavacCommand(Path classes) {
    return computeJavacCommand(computeReleaseVersionFeatureNumber(), classes);
  }

  protected JavacCommand computeJavacCommand(int release, Path classes) {
    var releaseOption = JavacCommand.ReleaseOption.of(release);
    var computedModuleSourcePath = ModuleSourcePathComputer.compute(space);
    return Command.javac()
        .modules(space.modules().names())
        .option(releaseOption)
        .option(computedModuleSourcePath.patterns())
        .option(computedModuleSourcePath.specifics())
        .option(computeModulePathsOption())
        .outputDirectoryForClasses(classes);
  }

  protected List<Path> computeRelease8ClassPaths(Path classes) {
    var paths = new ArrayList<Path>();
    space.modules().names().forEach(name -> paths.add(classes.resolve(name)));
    // TODO What about modular JAR files built for parent spaces?
    paths.addAll(PathSupport.list(bach.path().externalModules(), PathSupport::isJarFile));
    return List.copyOf(paths);
  }

  protected List<Path> computeRelease8JavaSourceFiles(DeclaredModule module) {
    var sources = module.folders().list(0, FolderType.SOURCES);
    return PathSupport.find(sources, 99, PathSupport::isJava8File);
  }

  protected List<JavacCommand> computeRelease8JavacCommands(Path classes) {
    var classPath =
        computeRelease8ClassPaths(classes).stream()
            .map(Path::toString)
            .collect(Collectors.joining(File.pathSeparator));
    var commands = new ArrayList<JavacCommand>();
    for (var module : space.modules()) {
      var java8Files = computeRelease8JavaSourceFiles(module);
      if (java8Files.isEmpty()) continue;
      var javac =
          Command.javac()
              .option(JavacCommand.ReleaseOption.of(8))
              .add("--class-path", classPath)
              .add("-implicit:none")
              .outputDirectoryForClasses(classes.resolve(module.name()))
              .addAll(java8Files);
      commands.add(javac);
    }
    return List.copyOf(commands);
  }

  protected JavacCommand computeMultiReleaseJavacCommand(
      int release, String module, Path classes, List<Path> javaSourceFiles) {
    return Command.javac()
        .option(JavacCommand.ReleaseOption.of(release))
        .option(computeModulePathsOption())
        .add("--class-path", classes.resolve(module))
        .add("-implicit:none")
        .outputDirectoryForClasses(computeOutputDirectoryForClasses(module, release))
        .addAll(javaSourceFiles);
  }

  protected List<JavacCommand> computeMultiReleaseJavacCommands(
      DeclaredModule module, Path classes) {
    var commands = new ArrayList<JavacCommand>();
    for (var release = 9; release <= Runtime.version().feature(); release++) {
      var roots = module.folders().list(release, FolderType.SOURCES);
      if (roots.isEmpty()) continue;
      var sources = PathSupport.find(roots, 99, PathSupport::isJavaFile);
      commands.add(computeMultiReleaseJavacCommand(release, module.name(), classes, sources));
    }
    return commands;
  }

  protected List<JavacCommand> computeMultiReleaseJavacCommands(Path classes) {
    var commands = new ArrayList<JavacCommand>();
    for (var module : space.modules())
      commands.addAll(computeMultiReleaseJavacCommands(module, classes));
    return commands;
  }

  protected JarCommand computeJarCommand(DeclaredModule module, Path classes, Path modules) {
    var name = module.name();
    var version = project.version().value();
    var file = modules.resolve(name + "@" + version + ".jar");

    var jar = Command.jar().mode("--create").file(file).add("--module-version", version);

    if (module.mainClass().isPresent()) jar = jar.main(module.mainClass().get());

    var folders = module.folders();
    var baseSources = folders.list(0, FolderType.SOURCES);
    if (!baseSources.isEmpty()) jar = jar.filesAdd(classes.resolve(name));
    var baseResources = folders.list(0, FolderType.RESOURCES);
    for (var resource : baseResources) jar = jar.filesAdd(resource);

    //    if (computedModulePatches.mode() == PatchMode.CLASSES) {
    //      var list = computedModulePatches.map().getOrDefault(name, List.of());
    //      jar = jar.forEach(list, (call, path) -> call.with("-C", path, "."));
    //    }

    // multi-release
    for (var release = 9; release <= Runtime.version().feature(); release++) {
      if (!folders.list(release, FolderType.SOURCES).isEmpty()) {
        jar = jar.filesAdd(release, computeOutputDirectoryForClasses(name, release));
      }
      for (var resource : folders.list(release, FolderType.RESOURCES)) {
        jar = jar.filesAdd(release, resource);
      }
    }

    return jar;
  }

  protected Path computeOutputDirectoryForClasses() {
    var computedNumber = computeReleaseVersionFeatureNumber();
    var release = computedNumber == 0 ? Runtime.version().feature() : computedNumber;
    return bach.path().workspace(space.name(), "classes-" + release);
  }

  protected Path computeOutputDirectoryForClasses(String module, int release) {
    return bach.path().workspace(space.name(), "classes-mr-" + release, module);
  }

  @Override
  public void run() {
    var modules = space.modules().values();
    if (modules.isEmpty()) {
      bach.logMessage("No %s module present".formatted(space.name()));
      return;
    }
    var size = modules.size();
    bach.logCaption("Compile %d %s module%s".formatted(size, space.name(), size == 1 ? "" : "s"));

    var classesDirectory = computeOutputDirectoryForClasses();
    var modulesDirectory = computeOutputDirectoryForModules(space);

    if (space.release() == 8) {
      bach.run(computeJavacCommand(9, classesDirectory));
      computeRelease8JavacCommands(classesDirectory).stream().parallel().forEach(bach::run);
    } else {
      bach.run(computeJavacCommand(classesDirectory));
    }

    var multiReleases = computeMultiReleaseJavacCommands(classesDirectory);
    if (multiReleases.size() >= 1) {
      bach.logCaption("Compile multi-release sources");
      multiReleases.stream().parallel().forEach(bach::run);
    }

    bach.run(Command.of("directories", "clean", modulesDirectory));
    modules.stream()
        .parallel()
        .map(module -> computeJarCommand(module, classesDirectory, modulesDirectory))
        .forEach(bach::run);
  }
}
