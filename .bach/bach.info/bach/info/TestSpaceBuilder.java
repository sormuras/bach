package bach.info;

import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Recording;
import com.github.sormuras.bach.Recordings;
import com.github.sormuras.bach.api.ProjectBuilderAPI;
import java.io.File;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public interface TestSpaceBuilder extends ProjectBuilderAPI {

  @Override
  default void buildProjectTestSpace() throws Exception {
    var module = "com.github.sormuras.bach";
    var names = List.of(module, "test.base", "test.integration", "test.projects");
    var mainModules = bach().folders().workspace("modules");
    var destination = bach().folders().workspace("classes", "test");
    var moduleSourcePath = String.join(File.pathSeparator, "./*/test/java", "./*/test/java-module");
    bach()
        .run(
            Command.javac()
                .add("--module", String.join(",", names))
                .add("--module-source-path", moduleSourcePath)
                .add("--module-path", List.of(mainModules, bach().folders().externalModules()))
                .add("--patch-module", module + "=" + module + "/main/java")
                .addAll(bach().project().spaces().test().tweaks().arguments("javac"))
                .add("-g")
                .add("-parameters")
                .add("-Xlint")
                .add("-d", destination))
        .requireSuccessful();

    var testModules = bach().folders().workspace("modules-test");
    Files.createDirectories(testModules);
    bach().run(names.stream().map(name -> jar(destination, name, testModules))).requireSuccessful();
    new Recordings(names.stream().map(name -> run(testModules, name)).toList()).requireSuccessful();
  }

  private Command<?> jar(Path classes, String module, Path modules) {
    var file = modules.resolve(module + "+test.jar");
    return Command.jar()
        .add("--verbose")
        .add("--create")
        .add("--file", file)
        .add("-C", classes.resolve(module), ".")
        .add("-C", bach().folders().root(module, "test/java"), ".");
  }

  private Recording run(Path testModules, String module) {
    var finder =
        ModuleFinder.of(
            testModules.resolve(module + "+test.jar"), // module under test
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
