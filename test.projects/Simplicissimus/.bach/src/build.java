import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;
import java.nio.file.Path;

class build {
  public static void main(String... args) {
    var classes = Path.of(".bach/workspace/classes");
    var modules = Path.of(".bach/workspace/modules");
    try (var bach = new Bach(args)) {
      bach.run(
          ToolCall.of("javac")
              .with("--module", "simplicissimus")
              .with("--module-source-path", "simplicissimus=.")
              .with("-d", classes));
      bach.run(ToolCall.of("directories", "clean", modules));
      bach.run(
          ToolCall.of("jar")
              .with("--create")
              .with("--file", modules.resolve("simplicissimus@99.jar"))
              .with("--module-version", "99")
              .with("-C", classes.resolve("simplicissimus"), "."));
      bach.run(
              ToolCall.java()
                  .with("--module-path", modules)
                  .with("--describe-module", "simplicissimus"))
          .output()
          .lines()
          .forEach(System.out::println);
    }
  }
}
