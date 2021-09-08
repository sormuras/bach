import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolFinder;
import com.github.sormuras.bach.command.Composer;
import com.github.sormuras.bach.conventional.ConventionalSpace;
import com.github.sormuras.bach.external.JUnit;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.stream.Stream;

class build {
  public static void main(String... args) {
    switch (System.getProperty("build", "default")) {
      default -> BuildWithBachApi.main(args);
      case "conventional" -> BuildWithConventionalApi.main(args);
      case "project" -> BuildWithProjectApi.main(args);
    }
  }

  static class BuildWithBachApi {
    public static void main(String... args) {
      try (var bach = new Bach(args)) {
        bach.logCaption("Compile Main Space");
        var mainClasses = Path.of(".bach/workspace/classes");
        var mainModules = Path.of(".bach/workspace/modules");
        bach.run(
            Command.javac()
                .modules("com.greetings", "org.astro")
                .moduleSourcePathAddPattern("./*/main/java")
                .add("-d", mainClasses));
        bach.run(ToolCall.of("directories", "clean", mainModules));
        Stream.of(
                Command.jar()
                    .mode("--create")
                    .file(mainModules.resolve("com.greetings@99.jar"))
                    .add("--module-version", "99")
                    .main("com.greetings.Main")
                    .filesAdd(mainClasses.resolve("com.greetings")),
                Command.jar()
                    .mode("--create")
                    .file(mainModules.resolve("org.astro@99.jar"))
                    .add("--module-version", "99")
                    .filesAdd(mainClasses.resolve("org.astro")))
            .parallel()
            .forEach(bach::run);
        bach.run(ToolCall.module(ModuleFinder.of(mainModules), "com.greetings"))
            .visit(bach.logbook()::print);

        bach.logCaption("Compile Test Space");
        var testClasses = Path.of(".bach/workspace/test-classes");
        var testModules = Path.of(".bach/workspace/test-modules");
        bach.run(
            Command.javac()
                .modules("test.modules")
                .moduleSourcePathAddPattern("./*/test/java")
                .add("--module-path", ".bach/workspace/modules")
                .add("-d", ".bach/workspace/test-classes"));
        bach.run(ToolCall.of("directories", "clean", testModules));
        bach.run(
            Command.jar()
                .mode("--create")
                .file(testModules.resolve("test.modules@99+test.jar"))
                .add("--module-version", "99+test")
                .main("test.modules.Main")
                .filesAdd(testClasses.resolve("test.modules")));
        var finder = ModuleFinder.of(testModules, mainModules);

        bach.logCaption("Execute Tests");
        bach.run(ToolCall.module(finder, "test.modules", 1, 2, 3));
        bach.run(ToolCall.of(ToolFinder.of(finder, true, "test.modules"), "test", 4, 5, 6));
      }
    }
  }

  static class BuildWithConventionalApi {
    public static void main(String... args) {
      try (var bach = new Bach(args)) {
        var mainSpace =
            ConventionalSpace.of("main")
                .modulesAdd("com.greetings", module -> module.main("com.greetings.Main"))
                .modulesAdd(
                    "org.astro", module -> module.resourcesAdd(Path.of("org.astro/main/java")));

        var mainBuilder =
            mainSpace.toBuilder(bach)
                .compile(javac -> javac.add("-Xlint").add("-Werror"), jar -> jar.verbose(true));

        mainBuilder.runModule("com.greetings", run -> run.add("stranger"));

        bach.logCaption("Perform automated checks");
        var grabber = bach.grabber(JUnit.version("5.8.0-RC1"));

        var testBuilder =
            mainBuilder
                .newDependentConventionalSpace("test")
                .modulesAdd("test.modules", module -> module.main("test.modules.Main"))
                .toBuilder(bach);

        testBuilder.grab(grabber, "org.junit.jupiter", "org.junit.platform.console");
        testBuilder.compile(javac -> javac.add("-g").add("-parameters"), Composer.identity());
        testBuilder.runModule("test.modules", run -> run.add(456));
        testBuilder.runTool("test", run -> run.add(123));

        testBuilder.runAllTests();

        bach.logCaption("Generate API documentation and link modules into a custom runtime image");
        mainBuilder.document(
            javadoc ->
                javadoc
                    .add("-encoding", "UTF-8")
                    .add("-windowtitle", "Window Title")
                    .add("-header", "API documentation header")
                    .add("-notimestamp")
                    .add("-use")
                    .add("-linksource")
                    .add("-Xdoclint:-missing")
                    .add("-Werror"));
        mainBuilder.link(jlink -> jlink.add("--launcher", "greet=com.greetings"));
      }
    }
  }

  static class BuildWithProjectApi {
    public static void main(String... args) {
      throw new UnsupportedOperationException("BuildWithProjectApi");
    }
  }
}
