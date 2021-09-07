import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.ToolCall;
import java.nio.file.Path;

class build {
  public static void main(String... args) {
    try (var bach = new Bach(args)) {
      bach.logCaption("Clean");
      bach.run(ToolCall.of("directories", "delete", Path.of(".bach", "workspace")));

      bach.logCaption("Compile Main Space");
      var mainModules = compileMainModules(bach);

      bach.logCaption("Compile Test Space");
      bach.run(
          Command.javac()
              .verbose(true)
              .modules("tests")
              .moduleSourcePathPatterns("./*/test/java")
              .add("-Xplugin:showPlugin")
              .add("--processor-module-path", mainModules)
              .add("-d", Path.of(".bach/workspace/test-classes")));

      bach.logCaption("Generate API Documentation");
      bach.run(
          Command.of("javadoc")
              .add("--module", "showcode")
              .add("--module-source-path", "./*/main/java")
              .add("-docletpath", mainModules.resolve("showcode@99.jar"))
              .add("-doclet", "showcode.ShowDoclet"));
    }
  }

  private static Path compileMainModules(Bach bach) {
    var classes = Path.of(".bach/workspace/classes");
    var modules = Path.of(".bach/workspace/modules");
    bach.run(
        Command.javac()
            .modules("showcode")
            .moduleSourcePathAddPattern("./*/main/java")
            .add("-d", classes));
    bach.run(ToolCall.of("directories", "clean", modules));
    bach.run(
        Command.jar()
            .mode("--create")
            .file(modules.resolve("showcode@99.jar"))
            .add("--module-version", "99")
            .filesAdd(classes.resolve("showcode")));
    return modules;
  }
}
