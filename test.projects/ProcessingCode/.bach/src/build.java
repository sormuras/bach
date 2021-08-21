import static com.github.sormuras.bach.Note.caption;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;
import java.nio.file.Path;

class build {
  public static void main(String... args) {
    try (var bach = new Bach(args)) {
      bach.log(caption("Clean"));
      bach.run(ToolCall.of("directories", "delete", Path.of(".bach", "workspace")));

      bach.log(caption("Compile Main Space"));
      var mainModules = compileMainModules(bach);

      bach.log(caption("Compile Test Space"));
      bach.run(
          ToolCall.of("javac")
              .with("-verbose")
              .with("--module", "tests")
              .with("--module-source-path", "./*/test/java")
              .with("-Xplugin:showPlugin")
              .with("--processor-module-path", mainModules)
              .with("-d", Path.of(".bach/workspace/test-classes")));

      bach.log(caption("Generate API Documentation"));
      bach.run(
          ToolCall.of("javadoc")
              .with("--module", "showcode")
              .with("--module-source-path", "./*/main/java")
              .with("-docletpath", mainModules.resolve("showcode@99.jar"))
              .with("-doclet", "showcode.ShowDoclet"));
    }
  }

  private static Path compileMainModules(Bach bach) {
    var classes = Path.of(".bach/workspace/classes");
    var modules = Path.of(".bach/workspace/modules");
    bach.run(
        ToolCall.of("javac")
            .with("--module", "showcode")
            .with("--module-source-path", "./*/main/java")
            .with("-d", classes));
    bach.run(ToolCall.of("directories", "clean", modules));
    bach.run(
        ToolCall.of("jar")
            .with("--create")
            .with("--file", modules.resolve("showcode@99.jar"))
            .with("--module-version", "99")
            .with("-C", classes.resolve("showcode"), "."));
    return modules;
  }
}
