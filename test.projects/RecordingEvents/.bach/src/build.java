import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolFinder;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;

class build {
  public static void main(String... args) {
    try (var bach = new Bach(args)) {
      bach.logCaption("Grab External Assets");
      bach.run("grab", grab -> grab.add(".bach/external.properties"));

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
        Command.javac()
            .modules("foo", "bar")
            .moduleSourcePathAddPattern("./*/test/java")
            .add("--module-path", bach.path().externalModules())
            .add("-d", classes));
    bach.run(ToolCall.of("directories", "clean", modules));
    bach.run(
        Command.jar()
            .mode("--create")
            .file(modules.resolve("foo@99+test.jar"))
            .add("--module-version", "99+test")
            .filesAdd(classes.resolve("foo")));
    bach.run(
        Command.jar()
            .mode("--create")
            .file(modules.resolve("bar@99+test.jar"))
            .add("--module-version", "99+test")
            .filesAdd(classes.resolve("bar")));
    return modules;
  }

  private static void executeTestModules(Bach bach, ModuleFinder finder) {
    bach.run(
        ToolCall.of(
            ToolFinder.of(finder, true, "foo"),
            Command.of("junit")
                .add("--select-module", "foo")
                .add("--reports-dir", bach.path().workspace("junit-foo"))));
    bach.run(
        ToolCall.of(
            ToolFinder.of(finder, true, "bar"),
            Command.of("junit")
                .add("--select-module", "bar")
                .add("--reports-dir", bach.path().workspace("junit-bar"))));
  }
}
