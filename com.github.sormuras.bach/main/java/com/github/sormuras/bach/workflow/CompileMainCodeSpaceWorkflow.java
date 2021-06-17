package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.api.DeclaredModule;
import com.github.sormuras.bach.api.SourceFolder;
import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.internal.Strings;
import com.github.sormuras.bach.tool.JarCall;
import com.github.sormuras.bach.tool.JavacCall;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class CompileMainCodeSpaceWorkflow extends BachWorkflow {

  public CompileMainCodeSpaceWorkflow(Bach bach) {
    super(bach);
  }

  /** Compile and jar main modules. */
  public void compile() {
    var main = bach().project().spaces().main();
    var modules = main.modules();
    if (modules.isEmpty()) {
      bach().log("Main module list is empty, nothing to do here.");
      return;
    }
    var s = modules.size() == 1 ? "" : "s";
    bach().say("Compile %d main module%s: %s".formatted(modules.size(), s, modules.toNames(", ")));

    var release = main.release();
    var feature = release != 0 ? release : Runtime.version().feature();
    var classes = bach().project().folders().workspace("classes-main-" + feature);

    var workspaceModules = bach().project().folders().modules(CodeSpace.MAIN);
    Paths.deleteDirectories(workspaceModules);
    if (feature == 8) {
      bach().run(javac(9, classes)).requireSuccessful();
      runJavacForJava8(classes);
    } else {
      bach().run(javac(release, classes)).requireSuccessful();
    }

    Paths.createDirectories(workspaceModules);
    var jars = new ArrayList<JarCall>();
    var javacs = new ArrayList<JavacCall>();
    for (var declaration : modules.map().values()) {
      for (var folder : declaration.sources().list()) {
        if (!folder.isTargeted()) continue;
        javacs.add(javac(declaration, folder, classes));
      }
      jars.add(jar(declaration, classes));
    }
    if (!javacs.isEmpty()) bach().run(javacs.stream().parallel()).requireSuccessful();
    bach().run(jars.stream().parallel()).requireSuccessful();
  }

  /**
   * {@return the {@code javac} call to compile all configured modules of the main space}
   *
   * @param release the Java feature release number to compile modules for
   */
  protected JavacCall javac(int release, Path classes) {
    var project = bach().project();
    var main = project.spaces().main();
    var tweaks = bach().project().tools().tweaks();
    return new JavacCall()
        .ifTrue(release != 0, javac -> javac.with("--release", release))
        .with("--module", main.modules().toNames(","))
        .with("--module-version", project.version())
        .forEach(
            main.modules().toModuleSourcePaths(false),
            (javac, path) -> javac.with("--module-source-path", path))
        .ifPresent(main.paths().pruned(), (javac, paths) -> javac.with("--module-path", paths))
        .withAll(tweaks.arguments(CodeSpace.MAIN, "javac"))
        .with("-d", classes);
  }

  /** {@return the {@code javac} call to compile a specific version of a multi-release module} */
  protected JavacCall javac(DeclaredModule local, SourceFolder folder, Path classes) {
    var name = local.name();
    var project = bach().project();
    var main = project.spaces().main();
    var release = folder.release();
    var javaSourceFiles = Paths.find(folder.path(), 99, Paths::isJavaFile);
    var tweaks = bach().project().tools().tweaks();
    return new JavacCall()
        .with("--release", release)
        .with("--module-version", project.version())
        .ifPresent(main.paths().pruned(), (javac, paths) -> javac.with("--module-path", paths))
        .with("--class-path", classes.resolve(name))
        .with("-implicit:none") // generate classes for explicitly referenced source files
        .withAll(tweaks.arguments(CodeSpace.MAIN, "javac"))
        .withAll(tweaks.arguments(CodeSpace.MAIN, "javac(" + name + ")"))
        .withAll(tweaks.arguments(CodeSpace.MAIN, "javac(" + release + ")"))
        .withAll(tweaks.arguments(CodeSpace.MAIN, "javac(" + name + "@" + release + ")"))
        .with("-d", computeMultiReleaseClassesDirectory(name, release))
        .withAll(javaSourceFiles);
  }

  private Path computeMultiReleaseClassesDirectory(String module, int release) {
    return bach().project().folders().workspace("classes-mr-" + release, module);
  }

  private void runJavacForJava8(Path classes) {
    var project = bach().project();
    var folders = project.folders();
    var main = project.spaces().main();
    var classPaths = new ArrayList<Path>();
    main.modules().toNames().forEach(name -> classPaths.add(classes.resolve(name)));
    classPaths.addAll(Paths.list(folders.externalModules(), Paths::isJarFile));
    var tweaks = bach().project().tools().tweaks();
    var javacs = new ArrayList<JavacCall>();
    for (var declaration : main.modules().map().values()) {
      var name = declaration.name();
      var sources = declaration.sources();
      var root = sources.list().isEmpty() ? folders.root() : sources.first().path();
      var java8Files = Paths.find(root, 99, CompileMainCodeSpaceWorkflow::isJava8File);
      if (java8Files.isEmpty()) continue; // skip aggregator module
      var release = main.release(); // 8
      var compileSources =
          new JavacCall()
              .with("--release", release)
              .with("--class-path", classPaths)
              .withAll(tweaks.arguments(CodeSpace.MAIN, "javac"))
              .withAll(tweaks.arguments(CodeSpace.MAIN, "javac(" + name + ")"))
              .withAll(tweaks.arguments(CodeSpace.MAIN, "javac(" + release + ")"))
              .withAll(tweaks.arguments(CodeSpace.MAIN, "javac(" + name + "@" + release + ")"))
              .with("-d", classes.resolve(name))
              .withAll(java8Files);
      javacs.add(compileSources);
    }
    if (javacs.isEmpty()) return; // no javac command collected
    bach().run(javacs.stream().parallel()).requireSuccessful();
  }

  /** Test supplied path for pointing to a Java 8 compilation unit. */
  private static boolean isJava8File(Path path) {
    var name = Strings.name(path);
    return !path.startsWith(".bach") // ignore all files in `.bach` directory
        && name.endsWith(".java")
        && !name.equals("module-info.java") // ignore module declaration compilation units
        && Files.isRegularFile(path);
  }

  protected JarCall jar(DeclaredModule declared, Path classes) {
    var name = declared.name();
    var project = bach().project();
    var file = project.folders().jar(CodeSpace.MAIN, name, project.version());
    var mainClass = declared.reference().descriptor().mainClass();
    var tweaks = project.tools().tweaks();
    var jar =
        new JarCall()
            .ifTrue(bach().options().verbose(), args -> args.with("--verbose"))
            .with("--create")
            .with("--file", file)
            .with("--module-version", project.version())
            .ifPresent(mainClass, (args, value) -> args.with("--main-class", value))
            .withAll(tweaks.arguments(CodeSpace.MAIN, "jar"))
            .withAll(tweaks.arguments(CodeSpace.MAIN, "jar(" + name + ")"));
    var baseClasses = classes.resolve(name);
    if (Files.isDirectory(baseClasses)) jar = jar.with("-C", baseClasses, ".");
    // include base resources
    for (var folder : declared.resources().list()) {
      if (folder.isTargeted()) continue; // handled later
      jar = jar.with("-C", folder.path(), ".");
    }
    // add (future) targeted classes and targeted resources in ascending order
    for (int release = 9; release <= Runtime.version().feature(); release++) {
      var paths = new ArrayList<Path>();
      var isSourceTargeted = declared.sources().stream(release).findAny().isPresent();
      if (isSourceTargeted) paths.add(computeMultiReleaseClassesDirectory(name, release));
      declared.resources().stream(release).map(SourceFolder::path).forEach(paths::add);
      if (paths.isEmpty()) continue;
      jar = jar.with("--release", release);
      for (var path : paths) jar = jar.with("-C", path, ".");
    }
    return jar;
  }
}
