package run.bach.project;

import java.lang.module.ModuleFinder;
import run.bach.Bach;
import run.bach.DeclaredModule;
import run.bach.Project;
import run.bach.ToolCall;
import run.bach.ToolFinder;
import run.bach.ToolOperator;

public class TestTool implements ToolOperator {
  public TestTool() {}

  @Override
  public String name() {
    return "test";
  }

  @Override
  public void run(Operation operation) {
    var bach = operation.bach();
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
              .with("--module-path", space.toModulePath(paths).orElse("."))
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
