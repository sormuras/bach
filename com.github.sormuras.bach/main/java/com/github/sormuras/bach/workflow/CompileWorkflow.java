package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Call;
import com.github.sormuras.bach.Workflow;
import com.github.sormuras.bach.call.CreateDirectoriesCall;
import com.github.sormuras.bach.call.JarCall;
import com.github.sormuras.bach.call.JavacCall;
import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.JavaRelease;
import com.github.sormuras.bach.project.ModulePatches;
import com.github.sormuras.bach.project.ModulePaths;
import com.github.sormuras.bach.project.ModuleSourcePaths;
import com.github.sormuras.bach.project.PatchMode;
import com.github.sormuras.bach.project.PathType;
import com.github.sormuras.bach.project.ProjectSpace;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CompileWorkflow extends Workflow {

  protected final ProjectSpace space;
  protected final ModuleSourcePaths computedModuleSourcePaths;
  protected final ModulePatches computedModulePatches;
  protected final ModulePaths computedModulePaths;

  public CompileWorkflow(Bach bach, ProjectSpace space) {
    super(bach);
    this.space = space;
    this.computedModuleSourcePaths = computeModuleSourcePaths();
    this.computedModulePatches = computeModulePatches();
    this.computedModulePaths = computeModulePaths();
  }

  protected ModuleSourcePaths computeModuleSourcePaths() {
    return space.moduleSourcePaths().orElseGet(space.modules()::toModuleSourcePaths);
  }

  protected ModulePatches computeModulePatches() {
    return space.modulePatches().orElseGet(ModulePatches::of);
  }

  protected ModulePaths computeModulePaths() {
    return space.modulePaths().orElseGet(ModulePaths::of);
  }

  protected Path computeMultiReleaseClassesDirectory(String module, int release) {
    return bach.folders().workspace("classes-mr-" + release, module);
  }

  @Override
  public void execute() {
    bach.execute(generateCallTree());
  }

  public Call.Tree generateCallTree() {
    if (space.modules().isEmpty()) return Call.tree("No %s module present".formatted(space.name()));
    return generateCallTree(DeclaredModuleFinder.of(space.modules()));
  }

  public Call.Tree generateCallTree(DeclaredModuleFinder finder) {
    var feature = space.release().map(JavaRelease::feature).orElse(Runtime.version().feature());
    var classes = bach.folders().workspace("classes" + space.suffix() + "-" + feature);
    var modules = bach.folders().workspace("modules" + space.suffix());

    var size = finder.size();
    return Call.tree("Compile %d %s module%s".formatted(size, space.name(), size == 1 ? "" : "s"))
        .with(generateJavacCall(finder.names().toList(), classes))
        .with(generateJava8CallTree(finder, classes))
        .with(generateTargetedCallTree(finder, classes))
        .with(Call.tree("Create archive directory", new CreateDirectoriesCall(modules)))
        .with(
            Call.tree(
                "Archive %d %s module%s".formatted(size, space.name(), size == 1 ? "" : "s"),
                finder
                    .modules()
                    .parallel()
                    .map(module -> generateJarCall(module, classes, modules))));
  }

  public JavacCall generateJavacCall(List<String> modules, Path classes) {
    var release = space.release().map(it -> it.feature() != 8 ? it : new JavaRelease(9));
    return new JavacCall()
        .ifPresent(release, JavacCall::withRelease)
        .withModule(modules)
        .withDirectoryForClasses(classes)
        .ifPresent(computedModuleSourcePaths.patterns(), JavacCall::withModuleSourcePathPatterns)
        .ifPresent(computedModuleSourcePaths.specifics(), JavacCall::withModuleSourcePathSpecifics)
        .ifPresent(computedModulePatches.map(), JavacCall::withPatchModules)
        .withEncoding(project.defaults().encoding())
        .ifPresent(computedModulePaths.pruned(), JavacCall::withModulePath);
  }

  public Call.Tree generateJava8CallTree(DeclaredModuleFinder finder, Path classes) {
    if (space.release().map(JavaRelease::feature).orElse(0) != 8)
      return Call.tree("No Java 8, no calls.");

    var classPaths = new ArrayList<Path>();
    finder.names().forEach(name -> classPaths.add(classes.resolve(name)));
    try {
      classPaths.addAll(Paths.list(bach.folders().externalModules(), Paths::isJarFile));
    } catch (Exception exception) {
      throw new RuntimeException("Listing external modules failed", exception);
    }

    var calls = new ArrayList<Call>();
    for (var module : finder.modules().toList()) {
      var name = module.name();
      var sources = module.paths().list(0, PathType.SOURCES);
      try {
        var java8Files = Paths.find(sources, 99, Paths::isJava8File);
        if (java8Files.isEmpty()) continue; // skip aggregator module
        var javac =
            new JavacCall()
                .withRelease(8)
                .withDirectoryForClasses(classes.resolve(name))
                .with("--class-path", classPaths)
                .withEncoding(project.defaults().encoding())
                .with("-implicit:none")
                .withAll(java8Files);
        calls.add(javac);
      } catch (Exception exception) {
        throw new RuntimeException("Find Java 8 source files failed", exception);
      }
    }
    return Call.tree("Compile sources for Java 8", calls.stream());
  }

  public Call.Tree generateTargetedCallTree(DeclaredModuleFinder finder, Path classes) {
    return Call.tree(
        "Compile sources for specified Java release",
        finder
            .modules()
            .parallel()
            .flatMap(module -> generateTargetedJavacCalls(module, classes).stream()));
  }

  public List<JavacCall> generateTargetedJavacCalls(DeclaredModule module, Path classes) {
    var javacCalls = new ArrayList<JavacCall>();
    for (var release = 9; release <= Runtime.version().feature(); release++) {
      var paths = module.paths().list(release, PathType.SOURCES);
      if (paths.isEmpty()) continue;
      try {
        var sources = Paths.find(paths, 99, Paths::isJavaFile);
        javacCalls.add(generateTargetedJavacCall(release, module.name(), classes, sources));
      } catch (Exception exception) {
        throw new RuntimeException("Find Java source files failed", exception);
      }
    }
    return javacCalls;
  }

  public JavacCall generateTargetedJavacCall(
      int release, String module, Path classes, List<Path> javaSourceFiles) {
    return new JavacCall()
        .withRelease(release)
        .withDirectoryForClasses(computeMultiReleaseClassesDirectory(module, release))
        .with("--class-path", classes.resolve(module))
        .ifPresent(computedModulePaths.pruned(), JavacCall::withModulePath)
        .withEncoding(project.defaults().encoding())
        .with("-implicit:none")
        .withAll(javaSourceFiles);
  }

  public JarCall generateJarCall(DeclaredModule module, Path classes, Path destination) {
    var descriptor = module.descriptor();
    var name = descriptor.name();
    var version = descriptor.version().orElse(project.version().value());
    var file = destination.resolve(name + "@" + version + space.suffix() + ".jar");

    var jar =
        new JarCall()
            .with("--create")
            .with("--file", file)
            .with("--module-version", version + space.suffix())
            .ifPresent(descriptor.mainClass(), (call, main) -> call.with("--main-class", main));

    var baseSources = module.paths().list(0, PathType.SOURCES);
    if (!baseSources.isEmpty()) jar = jar.withAllIn(classes.resolve(name));
    var baseResources = module.paths().list(0, PathType.RESOURCES);
    for (var resource : baseResources) jar = jar.with(resource);
    if (computedModulePatches.mode() == PatchMode.CLASSES) {
      var list = computedModulePatches.map().getOrDefault(name, List.of());
      jar = jar.forEach(list, (call, path) -> call.with("-C", path, "."));
    }
    // multi-release
    for (var release = 9; release <= Runtime.version().feature(); release++) {
      var sources = module.paths().list(release, PathType.SOURCES);
      var resources = module.paths().list(release, PathType.RESOURCES);
      if (sources.isEmpty() && resources.isEmpty()) continue;
      jar = jar.with("--release", release);
      if (!sources.isEmpty())
        jar = jar.withAllIn(computeMultiReleaseClassesDirectory(name, release));
      for (var resource : resources) jar = jar.with(resource);
    }

    return jar;
  }
}
