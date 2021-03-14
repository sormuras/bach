package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.tool.Jar;
import com.github.sormuras.bach.tool.Javac;
import com.github.sormuras.bach.util.Paths;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.List;

/** Methods related to building and running test modules. */
public interface ProjectBuildTestSpaceAPI extends API {

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
    if (bach().computeToolProvider("junit").isPresent()
        && bach().project().settings().tools().enabled("junit")) {
      say("Launch JUnit Platform for each module");
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
