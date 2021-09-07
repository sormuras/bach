import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolFinder;
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
        //        var main =
        //            bach.builder()
        //                .conventional("main")
        //                .withModule("com.greetings", module -> module.main("com.greetings.Main"))
        //                .withModule("org.astro", module ->
        // module.resource(Path.of("org.astro/main/java")));
        //        main.compile(javac -> javac.with("-Xlint").with("-Werror"), jar ->
        // jar.with("--verbose"));
        //        main.runModule("com.greetings", run -> run.with("stranger"));

        bach.logCaption("Perform automated checks");
        var grabber = bach.grabber(JUnit.version("5.8.0-RC1"));
        //        var test = main.dependentSpace("test").withModule("test.modules");
        //        test.grab(grabber, "org.junit.jupiter", "org.junit.platform.console");
        //        test.compile(javac -> javac.with("-g").with("-parameters"));
        //        test.runTool("special-test", run -> run.with(123));
        //        test.runAllTests(); // "test"

        bach.logCaption("Document API and link modules into a custom runtime image");
        //        main.document(
        //            javadoc ->
        //                javadoc
        //                    .with("-encoding", "UTF-8")
        //                    .with("-windowtitle", "Window Title")
        //                    .with("-header", "API documentation header")
        //                    .with("-notimestamp")
        //                    .with("-use")
        //                    .with("-linksource")
        //                    .with("-Xdoclint:-missing")
        //                    .with("-Werror"));
        //        main.link(jlink -> jlink.with("--launcher", "greet=com.greetings"));
      }
    }
  }

  static class BuildWithProjectApi {
    public static void main(String... args) {
      throw new UnsupportedOperationException("BuildWithProjectApi");
    }
  }
}
