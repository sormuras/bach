package run.bach.tool;

import java.io.PrintWriter;
import java.lang.module.ModuleFinder;
import java.util.spi.ToolProvider;
import run.bach.Project;
import run.bach.ProjectTool;
import run.duke.ToolCall;
import run.duke.Workbench;

public class TestTool extends ProjectTool {
  public static ToolCall test() {
    return ToolCall.of("test");
  }

  public TestTool() {}

  protected TestTool(Workbench workbench) {
    super(workbench);
  }

  @Override
  public final String name() {
    return "test";
  }

  @Override
  public ToolProvider provider(Workbench workbench) {
    return new TestTool(workbench);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var spaces = project().spaces();
    if (!spaces.names().contains("test")) return 0;
    var tester = new SpaceTester(spaces.space("test"));
    tester.runSpaceLauncher();
    tester.runJUnitPlatform();
    return 0;
  }

  class SpaceTester {
    final Project.Space space;

    SpaceTester(Project.Space space) {
      this.space = space;
    }

    void runSpaceLauncher() {
      for (var launcher : space.launchers()) runSpaceLauncher(launcher);
    }

    void runSpaceLauncher(String launcher) {
      var java =
          ToolCall.of("java")
              .with("--module-path", space.toRuntimeSpace().toModulePath(folders()).orElse("."))
              .with("--module", launcher);
      run(java);
    }

    void runJUnitPlatform() {
      if (find("junit").isEmpty()) return;
      for (var module : space.modules()) {
        runJUnitPlatform(module);
      }
    }

    void runJUnitPlatform(Project.DeclaredModule module) {
      var name = module.name();
      var paths = folders();
      var moduleFinder =
          ModuleFinder.of(
              paths.out("test", "modules", name + ".jar"),
              paths.out("main", "modules"),
              paths.out("test", "modules"),
              paths.externalModules());
      run(
          // TODO ToolFinder.ofModuleFinder(moduleFinder, true, name),
          ToolCall.of("junit")
              .with("--select-module", name)
              .with("--reports-dir", paths.out("test-reports", "junit-" + name)));
    }
  }
}
