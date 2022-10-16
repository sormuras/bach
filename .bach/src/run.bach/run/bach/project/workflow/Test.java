package run.bach.project.workflow;

import java.lang.module.ModuleFinder;
import java.util.List;
import run.bach.Bach;
import run.bach.ToolCall;
import run.bach.ToolFinder;
import run.bach.ToolOperator;
import run.bach.project.DeclaredModule;
import run.bach.project.ProjectSpace;

public class Test implements ToolOperator {

  static final String NAME = "test";

  public Test() {}

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

  record Tester(Bach bach, ProjectSpace space) {
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
