import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.ToolCall;
import java.nio.file.Path;

class build {
  public static void main(String... args) {
    var classes = Path.of(".bach/workspace/classes");
    var modules = Path.of(".bach/workspace/modules");
    try (var bach = new Bach(args)) {
      bach.run(
          Command.javac()
              .modules("simple")
              .moduleSourcePathAddSpecific("simple", Path.of("."))
              .add("-d", classes));
      bach.run(ToolCall.of("directories", "clean", modules));
      bach.run(
          Command.jar()
              .mode("--create")
              .file(modules.resolve("simple@1.0.1.jar"))
              .add("--module-version", "1.0.1")
              .main("simple.Main")
              .filesAdd(classes.resolve("simple"))
              .addAll(
                  "module-info.java",
                  "simple/Main.java",
                  "simple/Main.txt",
                  "simple/internal/Interface.java"));
      bach.run(Command.jar().mode("--describe-module").file(modules.resolve("simple@1.0.1.jar")))
          .output()
          .lines()
          .forEach(System.out::println);
      bach.run(ToolCall.java("--module-path", modules, "--describe-module", "simple"))
          .output()
          .lines()
          .forEach(System.out::println);
    }
  }
}
