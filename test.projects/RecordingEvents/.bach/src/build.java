import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolFinder;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;

class build {
  public static void main(String... args) {
    try (var bach = new Bach(args)) {
      bach.logCaption("Grab External Assets");
      bach.run("grab", grab -> grab.with(".bach/external.properties"));

      bach.logCaption("Compile Test Space");
      var testModules = compileTestModules(bach);

      bach.logCaption("Execute Test Modules");
      executeTestModules(bach, ModuleFinder.of(testModules, bach.path().externalModules()));
    }
  }

  private static Path compileTestModules(Bach bach) {
    var classes = bach.path().workspace("test-classes");
    var modules = bach.path().workspace("test-modules");
    bach.run(
        ToolCall.of("javac")
            .with("--module", "foo,bar")
            .with("--module-source-path", "./*/test/java")
            .with("--module-path", bach.path().externalModules())
            .with("-d", classes));
    bach.run(ToolCall.of("directories", "clean", modules));
    bach.run(
        ToolCall.of("jar")
            .with("--create")
            .with("--file", modules.resolve("foo@99+test.jar"))
            .with("--module-version", "99+test")
            .with("-C", classes.resolve("foo"), "."));
    bach.run(
        ToolCall.of("jar")
            .with("--create")
            .with("--file", modules.resolve("bar@99+test.jar"))
            .with("--module-version", "99+test")
            .with("-C", classes.resolve("bar"), "."));
    return modules;
  }

  private static void executeTestModules(Bach bach, ModuleFinder finder) {
    bach.run(
        ToolCall.of(ToolFinder.of(finder, true, "foo"), "junit")
            .with("--select-module", "foo")
            .with("--reports-dir", bach.path().workspace("junit-foo")));
    bach.run(
        ToolCall.of(ToolFinder.of(finder, true, "bar"), "junit")
            .with("--select-module", "bar")
            .with("--reports-dir", bach.path().workspace("junit-bar")));
  }
}
