import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolFinder;
import com.github.sormuras.bach.simple.SimpleSpace;
import com.github.sormuras.bach.workflow.WorkflowBuilder;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

class build {
  public static void main(String... args) {
    switch (System.getProperty("build", "simple")) {
      default -> BuildWithBachApi.main(args);
      case "simple" -> BuildWithSimpleApi.main(args);
      case "customizable" -> BuildWithCustomizableApi.main(args);
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

  static class BuildWithSimpleApi {
    public static void main(String... args) {
      try (var bach = new Bach(args)) {
        var main =
            SimpleSpace.of(bach, "main")
                .withModule("com.greetings", module -> module.main("com.greetings.Main"))
                .withModule("org.astro", module -> module.withResourcePath("org.astro/main/java"));

        main.compile(javac -> javac.add("-Xlint").add("-Werror"), jar -> jar.verbose(true));
        main.runModule("com.greetings", run -> run.add("stranger"));

        bach.logCaption("Perform automated checks");
        var test =
            main.newDependentSpace("test")
                .withModule("test.modules", module -> module.main("test.modules.Main"));

        test.compile(javac -> javac.add("-g").add("-parameters"));
        test.runModule("test.modules", run -> run.add(456));
        test.runTool("test", run -> run.add(123));
        test.runAllTests();

        bach.logCaption("Generate API documentation and link modules into a custom runtime image");
        main.document(
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
        main.link(jlink -> jlink.add("--launcher", "greet=com.greetings"));
      }
    }
  }

  static class BuildWithCustomizableApi {
    public static void main(String... args) {
      var project =
          Project.of("JigsawQuickStartWorldWithTests", "99")
              .withSpaces(
                  spaces ->
                      spaces
                          .withSpace(
                              "main",
                              main ->
                                  main.withModule(
                                          Path.of("com.greetings/main/java"),
                                          module -> module.withMainClass("com.greetings.Main"))
                                      .withModule("org.astro/main/java"))
                          .withSpace(
                              "test",
                              Set.of("main"),
                              test ->
                                  test.withModule("test.modules/test/java", "test.modules.Main")));
      try (var bach = new Bach()) {
        bach.logMessage("Build project %s".formatted(project.toNameAndVersion()));
        var builder = new WorkflowBuilder(bach, project);
        builder.compile();
        builder.runModule("com.greetings", 'I');
        builder.runModule(project.space("test"), "test.modules", run -> run.add("II"));
      }
    }
  }
}
