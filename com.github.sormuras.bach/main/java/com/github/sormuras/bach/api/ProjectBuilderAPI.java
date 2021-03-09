package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Recording;
import com.github.sormuras.bach.Recordings;
import com.github.sormuras.bach.internal.Strings;
import com.github.sormuras.bach.project.ModuleDeclaration;
import com.github.sormuras.bach.project.SourceFolder;
import com.github.sormuras.bach.tool.Jar;
import com.github.sormuras.bach.tool.Javac;
import com.github.sormuras.bach.util.Paths;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Methods related to building projects. */
public interface ProjectBuilderAPI {

  Bach bach();

  default void buildProject() throws Exception {
    var bach = bach();
    var project = bach.project();
    bach.print("Build %s %s", project.name(), project.version());
    if (bach.is(Options.Flag.VERBOSE)) bach.info();
    var start = Instant.now();
    if (bach.is(Options.Flag.STRICT)) bach.formatJavaSourceFiles(JavaFormatterAPI.Mode.VERIFY);
    bach.loadMissingExternalModules();
    bach.verifyExternalModules();
    buildProjectMainSpace();
    buildProjectTestSpace();
    bach.print("Build took %s", Strings.toString(Duration.between(start, Instant.now())));
  }

  /**
   * Build modules of the main code space.
   *
   * <ul>
   *   <li>{@code javac} + {@code jar}
   *   <li>TODO {@code javadoc}
   *   <li>TODO {@code jlink}
   *   <li>TODO {@code jpackage}
   * </ul>
   */
  default void buildProjectMainSpace() throws Exception {
    var main = bach().project().spaces().main();
    var modules = main.declarations();
    if (modules.isEmpty()) {
      bach().debug("Main module list is empty, nothing to build here.");
      return;
    }
    var s = modules.size() == 1 ? "" : "s";
    bach().print("Build %d main module%s: %s", modules.size(), s, modules.toNames(", "));

    var release = main.release();
    var feature = release != 0 ? release : Runtime.version().feature();
    var classes = bach().folders().workspace("classes-main", String.valueOf(feature));

    var workspaceModules = bach().folders().workspace("modules");
    Paths.deleteDirectories(workspaceModules);
    if (feature == 8) {
      bach().run(buildProjectMainJavac(9, classes)).requireSuccessful();
      buildProjectMainSpaceClassesForJava8(classes);
    } else {
      bach().run(buildProjectMainJavac(release, classes)).requireSuccessful();
    }

    Paths.createDirectories(workspaceModules);
    var jars = new ArrayList<Jar>();
    var javacs = new ArrayList<Javac>();
    for (var declaration : modules.map().values()) {
      for (var folder : declaration.sources().list()) {
        if (!folder.isTargeted()) continue;
        javacs.add(buildProjectMainJavac(declaration, folder, classes));
      }
      jars.add(buildProjectMainJar(declaration, classes));
    }
    if (!javacs.isEmpty()) bach().run(javacs.stream()).requireSuccessful();
    bach().run(jars.stream()).requireSuccessful();
  }

  default void buildProjectMainSpaceClassesForJava8(Path classes) {
    var project = bach().project();
    var main = project.spaces().main();
    var classPaths = new ArrayList<Path>();
    main.declarations().toNames().forEach(name -> classPaths.add(classes.resolve(name)));
    classPaths.addAll(Paths.list(bach().folders().externalModules(), Paths::isJarFile));
    var javacs = new ArrayList<Javac>();
    for (var declaration : main.declarations().map().values()) {
      var name = declaration.name();
      var java8Files = Paths.find(declaration.sources().first().path(), 99, Paths::isJava8File);
      if (java8Files.isEmpty()) continue; // skip aggregator module
      var compileSources =
          Command.javac()
              .add("--release", main.release()) // 8
              .add("--class-path", classPaths)
              .ifTrue(bach().is(Options.Flag.STRICT), javac -> javac.add("-Xlint").add("-Werror"))
              .addAll(main.tweaks().arguments("javac"))
              .add("-d", classes.resolve(name))
              .addAll(java8Files);
      javacs.add(compileSources);
    }
    bach().run(javacs.stream()).requireSuccessful();
  }

  /**
   * {@return the {@code javac} call to compile all configured modules of the main space}
   *
   * @param release the Java feature release number to compile modules for
   */
  default Javac buildProjectMainJavac(int release, Path classes) {
    var project = bach().project();
    var main = project.spaces().main();
    return Command.javac()
        .ifTrue(release != 0, javac -> javac.add("--release", release))
        .add("--module", main.declarations().toNames(","))
        .add("--module-version", project.version())
        .forEach(
            main.declarations().toModuleSourcePaths(false),
            (javac, path) -> javac.add("--module-source-path", path))
        .ifPresent(main.modulePaths().pruned(), (javac, paths) -> javac.add("--module-path", paths))
        .ifTrue(bach().is(Options.Flag.STRICT), javac -> javac.add("-Xlint").add("-Werror"))
        .addAll(main.tweaks().arguments("javac"))
        .add("-d", classes);
  }

  /** {@return the {@code javac} call to compile a specific version of a multi-release module} */
  default Javac buildProjectMainJavac(
      ModuleDeclaration declaration, SourceFolder folder, Path classes) {
    var name = declaration.name();
    var project = bach().project();
    var main = project.spaces().main();
    var release = folder.release();
    var javaSourceFiles = Paths.find(folder.path(), 99, Paths::isJavaFile);
    return Command.javac()
        .add("--release", release)
        .add("--module-version", project.version())
        .add("--patch-module", name + '=' + classes.resolve(name))
        .ifPresent(main.modulePaths().pruned(), (javac, paths) -> javac.add("--module-path", paths))
        .add("-implicit:none") // generate classes for explicitly referenced source files
        .addAll(main.tweaks().arguments("javac"))
        .addAll(main.tweaks().arguments("javac(" + name + ")"))
        .addAll(main.tweaks().arguments("javac(" + release + ")"))
        .addAll(main.tweaks().arguments("javac(" + name + "@" + release + ")"))
        .add("-d", buildProjectMultiReleaseClasses(name, release))
        .addAll(javaSourceFiles);
  }

  default Jar buildProjectMainJar(ModuleDeclaration declaration, Path classes) {
    var project = bach().project();
    var main = project.spaces().main();
    var name = declaration.name();
    var file = bach().folders().workspace("modules", buildProjectMainJarFileName(name));
    var jar =
        Command.jar()
            .ifTrue(bach().is(Options.Flag.VERBOSE), command -> command.add("--verbose"))
            .add("--create")
            .add("--file", file)
            .addAll(main.tweaks().arguments("jar"))
            .addAll(main.tweaks().arguments("jar(" + name + ")"));
    var baseClasses = classes.resolve(name);
    if (Files.isDirectory(baseClasses)) jar = jar.add("-C", baseClasses, ".");
    // include base resources
    for (var folder : declaration.resources().list()) {
      if (folder.isTargeted()) continue; // handled later
      jar = jar.add("-C", folder.path(), ".");
    }
    // add targeted classes and targeted resources in ascending order
    for (int release = 9; release <= Runtime.version().feature(); release++) {
      var paths = new ArrayList<Path>();
      var pathN = buildProjectMultiReleaseClasses(name, release);
      declaration.sources().targets(release).ifPresent(it -> paths.add(pathN));
      declaration.resources().targets(release).ifPresent(it -> paths.add(it.path()));
      if (paths.isEmpty()) continue;
      jar = jar.add("--release", release);
      for (var path : paths) jar = jar.add("-C", path, ".");
    }
    return jar;
  }

  default String buildProjectMainJarFileName(String module) {
    return module + '@' + bach().project().versionNumberAndPreRelease() + ".jar";
  }

  private Path buildProjectMultiReleaseClasses(String module, int release) {
    return bach().folders().workspace("classes-mr", String.valueOf(release), module);
  }

  /**
   * Build modules of the test code space and launch JUnit Platform for each of them.
   *
   * <ul>
   *   <li>{@code javac}
   *   <li>{@code jar}
   *   <li>{@code junit}
   * </ul>
   */
  default void buildProjectTestSpace() {
    var test = bach().project().spaces().test();
    var modules = test.declarations();
    if (modules.isEmpty()) {
      bach().debug("Test module list is empty, nothing to build here.");
      return;
    }
    var s = modules.size() == 1 ? "" : "s";
    bach().print("Build %d test module%s: %s", modules.size(), s, modules.toNames(", "));

    var testClasses = bach().folders().workspace("classes-test");
    var testModules = bach().folders().workspace("modules-test");

    Paths.deleteDirectories(testModules);
    bach().run(buildProjectTestJavac(testClasses)).requireSuccessful();

    Paths.createDirectories(testModules);
    var names = modules.toNames().toList();
    var jars = names.stream().map(name -> buildProjectTestJar(testModules, name, testClasses));
    bach().run(jars).requireSuccessful();
    var runs = names.stream().map(name -> buildProjectTestJUnitRun(testModules, name));
    new Recordings(runs.toList()).requireSuccessful();
  }

  default Javac buildProjectTestJavac(Path classes) {
    var main = bach().project().spaces().main();
    var mainModules = bach().folders().workspace("modules");
    var test = bach().project().spaces().test();
    var tests = test.declarations();
    return Command.javac()
        .add("--module", tests.toNames(","))
        .forEach(
            tests.toModuleSourcePaths(false),
            (javac, path) -> javac.add("--module-source-path", path))
        .add("--module-path", List.of(mainModules, bach().folders().externalModules()))
        .forEach(
            tests.toModulePatches(main.declarations()).entrySet(),
            (javac, patch) -> javac.add("--patch-module", patch.getKey() + '=' + patch.getValue()))
        .addAll(test.tweaks().arguments("javac"))
        .add("-d", classes);
  }

  default String buildProjectTestJarFileName(String module) {
    return module + '@' + bach().project().versionNumberAndPreRelease() + "+test.jar";
  }

  default Jar buildProjectTestJar(Path testModules, String module, Path classes) {
    return Command.jar()
        .add("--verbose")
        .add("--create")
        .add("--file", testModules.resolve(buildProjectTestJarFileName(module)))
        .add("-C", classes.resolve(module), ".")
        .add("-C", bach().folders().root(module, "test/java"), ".");
  }

  default Recording buildProjectTestJUnitRun(Path testModules, String module) {
    var finder =
        ModuleFinder.of(
            testModules.resolve(buildProjectTestJarFileName(module)), // module under test
            bach().folders().workspace("modules"), // main modules
            testModules, // (more) test modules
            bach().folders().externalModules());

    var junit =
        Command.of("junit")
            .add("--select-module", module)
            .addAll(bach().project().spaces().test().tweaks().arguments("junit"))
            .addAll(bach().project().spaces().test().tweaks().arguments("junit(" + module + ")"))
            .add("--reports-dir", bach().folders().workspace("reports", "junit", module));

    return bach().run(junit, finder, module);
  }
}
