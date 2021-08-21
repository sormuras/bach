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
              .with("--module", "simple")
              .with("--module-source-path", "simple=.")
              .with("-d", classes));
      bach.run(ToolCall.of("directories", "clean", modules));
      bach.run(
          ToolCall.of("jar")
              .with("--create")
              .with("--file", modules.resolve("simple@1.0.1.jar"))
              .with("--module-version", "1.0.1")
              .with("--main-class", "simple.Main")
              .with("-C", classes.resolve("simple"), ".")
              .withAll(
                  "module-info.java",
                  "simple/Main.java",
                  "simple/Main.txt",
                  "simple/internal/Interface.java"));
      bach.run(
              ToolCall.of("jar")
                  .with("--describe-module")
                  .with("--file", modules.resolve("simple@1.0.1.jar")))
          .output()
          .lines()
          .forEach(System.out::println);
      bach.run(ToolCall.java().with("--module-path", modules).with("--describe-module", "simple"))
          .output()
          .lines()
          .forEach(System.out::println);
    }
  }
}
