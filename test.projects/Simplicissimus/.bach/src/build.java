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
              .modules("simplicissimus")
              .moduleSourcePathAddSpecific("simplicissimus", Path.of("."))
              .add("-d", classes));
      bach.run(ToolCall.of("directories", "clean", modules));
      bach.run(
          Command.jar()
              .mode("--create")
              .file(modules.resolve("simplicissimus@99.jar"))
              .add("--module-version", "99")
              .filesAdd(classes.resolve("simplicissimus")));
      bach.run(ToolCall.java("--module-path", modules, "--describe-module", "simplicissimus"))
          .output()
          .lines()
          .forEach(System.out::println);
    }
  }
}
