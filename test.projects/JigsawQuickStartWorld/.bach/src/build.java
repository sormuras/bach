import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;
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
      var classes = Path.of(".bach/workspace/classes");
      var modules = Path.of(".bach/workspace/modules");
      try (var bach = new Bach(args)) {
        bach.run(
            ToolCall.of("javac")
                .with("--module", "com.greetings,org.astro")
                .with("--module-source-path", ".")
                .with("-d", classes));

        bach.run(ToolCall.of("directories", "clean", modules));

        Stream.of(
                ToolCall.of("jar")
                    .with("--create")
                    .with("--file", modules.resolve("com.greetings@99.jar"))
                    .with("--module-version", "99")
                    .with("--main-class", "com.greetings.Main")
                    .with("-C", classes.resolve("com.greetings"), "."),
                ToolCall.of("jar")
                    .with("--create")
                    .with("--file", modules.resolve("org.astro@99.jar"))
                    .with("--module-version", "99")
                    .with("-C", classes.resolve("org.astro"), "."))
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
        var space = bach.builder().conventional("com.greetings", "org.astro");
        space.compile();
        space.runModule("com.greetings", run -> run.with("fun"));
      }
    }
  }

  static class BuildWithProjectApi {
    public static void main(String... args) {
      throw new UnsupportedOperationException("BuildWithProjectApi");
    }
  }
}
