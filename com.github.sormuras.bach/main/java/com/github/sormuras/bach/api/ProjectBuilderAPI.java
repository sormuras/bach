package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.internal.Strings;
import com.github.sormuras.bach.project.ModuleDeclaration;
import com.github.sormuras.bach.project.SourceFolder;
import com.github.sormuras.bach.tool.JDeps;
import com.github.sormuras.bach.tool.JLink;
import com.github.sormuras.bach.tool.Jar;
import com.github.sormuras.bach.tool.Javac;
import com.github.sormuras.bach.tool.Javadoc;
import com.github.sormuras.bach.util.Paths;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Methods related to building projects. */
public interface ProjectBuilderAPI extends API {

  default void buildProject() throws Exception {
    var bach = bach();
    say("Build %s %s", bach.project().name(), bach.project().version());
    if (bach.is(Options.Flag.VERBOSE)) bach.info();
    var start = Instant.now();
    if (bach.is(Options.Flag.STRICT)) bach.formatJavaSourceFiles(JavaFormatterAPI.Mode.VERIFY);
    bach.loadMissingExternalModules();
    bach.verifyExternalModules();
    buildProjectMainSpace();
    buildProjectTestSpace();
    say("Build took %s", Strings.toString(Duration.between(start, Instant.now())));
  }

  /**
   * Build modules of the main code space.
   *
   * <ul>
   *   <li>{@code javac} + {@code jar}
   *   <li>{@code jdeps}
   *   <li>{@code javadoc}
   *   <li>TODO {@code jlink}
   *   <li>TODO {@code jpackage}
   * </ul>
   */
  default void buildProjectMainSpace() throws Exception {
    var main = bach().project().spaces().main();
    var modules = main.declarations();
    if (modules.isEmpty()) {
      log("Main module list is empty, nothing to build here.");
      return;
    }
    var s = modules.size() == 1 ? "" : "s";
    say("Build %d main module%s: %s", modules.size(), s, modules.toNames(", "));

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

    var jdeps = new ArrayList<JDeps>();
    for (var declaration : modules.map().values()) {
      jdeps.add(buildProjectMainJDeps(declaration));
    }
    bach().run(jdeps.stream()).requireSuccessful();

    var api = bach().folders().workspace("documentation", "api");
    bach().run(buildProjectMainJavadoc(api)).requireSuccessful();
    if (Files.isDirectory(api)) bach().run(buildProjectMainJavadocJar(api)).requireSuccessful();

    var image = bach().folders().workspace("image");
    Paths.deleteDirectories(image);
    bach().run(buildProjectMainJLink(image));
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

  private Path buildProjectMultiReleaseClasses(String module, int release) {
    return bach().folders().workspace("classes-mr", String.valueOf(release), module);
  }

  default Jar buildProjectMainJar(ModuleDeclaration declaration, Path classes) {
    var project = bach().project();
    var main = project.spaces().main();
    var name = declaration.name();
    var file = bach().folders().workspace("modules", buildProjectMainJarFileName(name));
    var mainClass = declaration.reference().descriptor().mainClass();
    var jar =
        Command.jar()
            .ifTrue(bach().is(Options.Flag.VERBOSE), command -> command.add("--verbose"))
            .add("--create")
            .add("--file", file)
            .ifPresent(mainClass, (args, value) -> args.add("--main-class", value))
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

  default JDeps buildProjectMainJDeps(ModuleDeclaration declaration) {
    var folders = bach().folders();
    return Command.jdeps()
        .add("--check", declaration.name())
        .add("--multi-release", "BASE")
        .add("--module-path", List.of(folders.workspace("modules"), folders.externalModules()));
  }

  default Javadoc buildProjectMainJavadoc(Path destination) {
    var project = bach().project();
    var main = project.spaces().main();
    return Command.javadoc()
        .add("--module", main.declarations().toNames(","))
        .forEach(
            main.declarations().toModuleSourcePaths(false),
            (javac, path) -> javac.add("--module-source-path", path))
        .ifPresent(
            main.modulePaths().pruned(), (javadoc, paths) -> javadoc.add("--module-path", paths))
        .ifTrue(bach().is(Options.Flag.STRICT), javadoc -> javadoc.add("-Xdoclint").add("-Werror"))
        .addAll(main.tweaks().arguments("javadoc"))
        .add("-d", destination);
  }

  /** {@return the jar call generating the API documentation archive} */
  default Jar buildProjectMainJavadocJar(Path api) {
    var project = bach().project();
    var file = project.name() + "-api-" + project.version() + ".zip";
    return Command.jar()
        .add("--create")
        .add("--file", api.getParent().resolve(file))
        .add("--no-manifest")
        .add("-C", api, ".");
  }

  default JLink buildProjectMainJLink(Path image) {
    var project = bach().project();
    var main = project.spaces().main();
    var test = project.spaces().test();
    return Command.jlink()
        .add("--add-modules", main.declarations().toNames(","))
        .ifPresent(test.modulePaths().pruned(), (jlink, paths) -> jlink.add("--module-path", paths))
        .addAll(main.tweaks().arguments("jlink"))
        .add("--output", image);
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
      log("Test module list is empty, nothing to build here.");
      return;
    }
    var s = modules.size() == 1 ? "" : "s";
    say("Build %d test module%s: %s", modules.size(), s, modules.toNames(", "));

    var testClasses = bach().folders().workspace("classes-test");
    var testModules = bach().folders().workspace("modules-test");

    Paths.deleteDirectories(testModules);
    bach().run(buildProjectTestJavac(testClasses)).requireSuccessful();

    Paths.createDirectories(testModules);
    var names = modules.toNames().toList();
    var jars = names.stream().map(name -> buildProjectTestJar(testModules, name, testClasses));
    bach().run(jars).requireSuccessful();
    if (bach().computeToolProvider("junit").isPresent()) {
      var runs = names.stream().map(name -> buildProjectTestJUnitRun(testModules, name));
      new Logbook.Runs(runs.toList()).requireSuccessful();
    } else {
      var message =
          """

            Tool 'junit' not found!

          Either add `requires static org.junit.platform.console;` in one of the
          (test) module declarations or configure "org.junit.platform.console"
          into the list of required module names in the project declaration, like:

              @ProjectInfo(
                requires = {..., "org.junit.platform.console", ...}
              )
              module bach.info {...}
          """;
      say(message);
    }
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

  default Logbook.Run buildProjectTestJUnitRun(Path testModules, String module) {
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
