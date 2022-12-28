package run.bach.tool;

import java.io.PrintWriter;
import java.lang.module.ModuleFinder;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import run.bach.Bach;
import run.bach.Project;
import run.bach.internal.ModulesSupport;
import run.duke.ToolCall;

public class TestTool implements Bach.Operator {
  public static ToolCall test() {
    return ToolCall.of("test");
  }

  public TestTool() {}

  @Override
  public final String name() {
    return "test";
  }

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var spaces = bach.project().spaces();
    if (!spaces.names().contains("test")) return 0;
    var tester = new SpaceTester(bach, spaces.space("test"));
    tester.runSpaceLauncher();
    tester.runJUnitPlatform();
    return 0;
  }

  public static class SpaceTester {
    final Bach bach;
    final Project.Space space;

    public SpaceTester(Bach bach, Project.Space space) {
      this.bach = bach;
      this.space = space;
    }

    void runSpaceLauncher() {
      for (var launcher : space.launchers()) runSpaceLauncher(launcher);
    }

    void runSpaceLauncher(String launcher) {
      var folders = bach.folders();
      var java =
          ToolCall.of("java")
              .with("--module-path", space.toRuntimeSpace().toModulePath(folders).orElse("."))
              .with("--module", launcher);
      bach.run(java);
    }

    void runJUnitPlatform() {
      if (bach.find("junit").isEmpty()) return;
      for (var module : space.modules()) {
        runJUnitPlatform(module);
      }
    }

    void runJUnitPlatform(Project.DeclaredModule module) {
      var folders = bach.folders();
      var name = module.name();
      var finder =
          ModuleFinder.of(
              folders.out("test", "modules", name + ".jar"),
              folders.out("main", "modules"),
              folders.out("test", "modules"),
              folders.externalModules());
      var layer = ModulesSupport.buildModuleLayer(finder, name);
      layer.findLoader(name).setDefaultAssertionStatus(true);
      var junit =
          ServiceLoader.load(layer, ToolProvider.class).stream()
              .map(ServiceLoader.Provider::get)
              .filter(provider -> provider.name().equals("junit"))
              .findFirst()
              .orElseThrow();
      bach.run(
          ToolCall.of(junit)
              .with("--select-module", name)
              .with("--reports-dir", folders.out("test-reports", "junit-" + name)));
    }
  }
}
