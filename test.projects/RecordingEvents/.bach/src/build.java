import static com.github.sormuras.bach.Note.caption;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Call;
import com.github.sormuras.bach.ToolFinder;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;

class build {
  public static void main(String... args) {
    try (var bach = new Bach(args)) {
      bach.log(caption("Restore External Assets"));
      bach.run("restore", ".bach/external.properties");

      bach.log(caption("Compile Test Space"));
      var testModules = compileTestModules(bach);

      bach.log(caption("Execute Test Modules"));
      executeTestModules(bach, ModuleFinder.of(testModules, Path.of(".bach/external-modules")));
    }
  }

  private static Path compileTestModules(Bach bach) {
    var classes = Path.of(".bach/workspace/test-classes");
    var modules = Path.of(".bach/workspace/test-modules");
    bach.run(
        Call.tool("javac")
            .with("--module", "foo,bar")
            .with("--module-source-path", "./*/test/java")
            .with("--module-path", ".bach/external-modules")
            .with("-d", ".bach/workspace/test-classes"));
    bach.run(Call.tool("directories", "clean", modules));
    bach.run(
        Call.tool("jar")
            .with("--create")
            .with("--file", modules.resolve("foo@99+test.jar"))
            .with("--module-version", "99+test")
            .with("-C", classes.resolve("foo"), "."));
    bach.run(
        Call.tool("jar")
            .with("--create")
            .with("--file", modules.resolve("bar@99+test.jar"))
            .with("--module-version", "99+test")
            .with("-C", classes.resolve("bar"), "."));
    return modules;
  }

  private static void executeTestModules(Bach bach, ModuleFinder finder) {
    bach.run(
        Call.tool(ToolFinder.of(finder, true, "foo"), "junit")
            .with("--select-module", "foo")
            .with("--reports-dir", Path.of(".bach/workspace/junit-foo")));
    bach.run(
        Call.tool(ToolFinder.of(finder, true, "bar"), "junit")
            .with("--select-module", "bar")
            .with("--reports-dir", Path.of(".bach/workspace/junit-bar")));
  }
}
