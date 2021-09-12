import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.conventional.ConventionalSpace;
import com.github.sormuras.bach.customizable.CustomizableBuilder;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.stream.Stream;

class build {
  public static void main(String... args) {
    switch (System.getProperty("build", "customizable")) {
      default -> BuildWithBachApi.main(args);
      case "conventional" -> BuildWithConventionalApi.main(args);
      case "customizable" -> BuildWithCustomizableApi.main(args);
    }
  }

  static class BuildWithBachApi {
    public static void main(String... args) {
      var classes = Path.of(".bach/workspace/classes");
      var modules = Path.of(".bach/workspace/modules");
      try (var bach = new Bach(args)) {
        bach.run(
            Command.javac()
                .modules("com.greetings", "org.astro")
                .moduleSourcePathAddPattern(".")
                .add("-d", classes));

        bach.run(ToolCall.of("directories", "clean", modules));

        Stream.of(
                Command.jar()
                    .mode("--create")
                    .file(modules.resolve("com.greetings@99.jar"))
                    .add("--module-version", "99")
                    .main("com.greetings.Main")
                    .filesAdd(classes.resolve("com.greetings")),
                Command.jar()
                    .mode("--create")
                    .file(modules.resolve("org.astro@99.jar"))
                    .add("--module-version", "99")
                    .filesAdd(classes.resolve("org.astro")))
            .parallel()
            .forEach(bach::run);

        bach.run(ToolCall.module(ModuleFinder.of(modules), "com.greetings"))
            .visit(bach.logbook()::print);
      }
    }
  }

  static class BuildWithConventionalApi {
    public static void main(String... args) {
      try (var bach = new Bach(args)) {
        var space =
            ConventionalSpace.of(bach)
                .modulesAddModule("com.greetings", module -> module.main("com.greetings.Main"))
                .modulesAddModule("org.astro");
        space.compile();
        space.runModule("com.greetings", run -> run.add("fun"));
      }
    }
  }

  static class BuildWithCustomizableApi {
    public static void main(String... args) {
      var project =
          Project.of("JigsawQuickStartWorld", "99")
              .withSpaces(
                  spaces ->
                      spaces.withSpace(
                          "main",
                          main ->
                              main.withModule(
                                      Path.of("com.greetings"),
                                      module -> module.withMainClass("com.greetings.Main"))
                                  .withModule("org.astro")));
      try (var bach = new Bach()) {
        bach.logMessage("Build project %s".formatted(project.toNameAndVersion()));
        var builder = new CustomizableBuilder(bach, project);
        builder.compile();
        builder.runModule("com.greetings", run -> run.add("fun"));
      }
    }
  }
}
