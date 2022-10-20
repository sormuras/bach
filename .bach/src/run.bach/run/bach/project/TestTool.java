package run.bach.project;

import java.lang.module.ModuleFinder;
import java.util.List;
import run.bach.Bach;
import run.bach.DeclaredModule;
import run.bach.Project;
import run.bach.ToolCall;
import run.bach.ToolFinder;
import run.bach.ToolOperator;

public class TestTool implements ToolOperator {

  static final String NAME = "test";

  public TestTool() {}

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    var tester = new Tester(bach, bach.project().spaces().test());
    tester.runSpaceLauncher();
    tester.runJUnitPlatform();
  }

  record Tester(Bach bach, Project.Space space) {
    void runSpaceLauncher() {
      var launcher = space.launcher();
      if (launcher.isEmpty()) return;
      var paths = bach.paths();
      var java =
          ToolCall.of("java")
              .with("--module-path", paths.out("test", "modules"))
              .with("--module", launcher.get());
      bach.run(java);
    }

    void runJUnitPlatform() {
      if (bach.tools().finders().findFirst("junit").isEmpty()) return;
      for (var module : space.modules()) {
        runJUnitPlatform(module);
      }
    }

    void runJUnitPlatform(DeclaredModule module) {
      var name = module.name();
      var paths = bach.paths();
      var moduleFinder =
          ModuleFinder.of(
              paths.out("test", "modules", name + ".jar"),
              paths.out("main", "modules"),
              paths.out("test", "modules"),
              paths.externalModules());
      bach.run(
          ToolFinder.ofModuleFinder(moduleFinder, true, name),
          ToolCall.of("junit")
              .with("--select-module", name)
              .with("--reports-dir", paths.out("test-reports", "junit-" + name)),
          System.Logger.Level.INFO);
    }
  }
}
