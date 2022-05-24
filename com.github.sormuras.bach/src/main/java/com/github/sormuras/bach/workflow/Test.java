package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolCallTweak;
import com.github.sormuras.bach.ToolFinder;
import com.github.sormuras.bach.ToolOperator;
import com.github.sormuras.bach.ToolRunner;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.ProjectSpace;
import java.io.PrintWriter;
import java.lang.module.ModuleFinder;

public class Test implements ToolOperator {

  static final String NAME = "test";

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var tester = new Tester(bach, bach.project().spaces().test(), out, err);
    tester.runSpaceLauncher();
    tester.runJUnitPlatform();
    return 0;
  }

  record Tester(Bach bach, ProjectSpace space, PrintWriter out, PrintWriter err) {
    void runSpaceLauncher() {
      var launcher = space.launcher();
      if (launcher.isEmpty()) return;
      var paths = bach.configuration().paths();
      var java =
          ToolCall.of("java")
              .with("--module-path", paths.out("test", "modules"))
              .with("--module", launcher.get());
      bach.run(java);
    }

    void runJUnitPlatform() {
      if (bach.configuration().finder().find("junit").isEmpty()) return;
      for (var module : space.modules()) {
        runJUnitPlatform(module);
      }
    }

    void runJUnitPlatform(DeclaredModule module) {
      var name = module.name();
      bach.run("banner", "Execute tests declared in module " + name);
      var paths = bach.configuration().paths();
      var moduleFinder =
          ModuleFinder.of(
              paths.out("test", "modules", name + ".jar"),
              paths.out("main", "modules"),
              paths.out("test", "modules"),
              paths.externalModules());
      bach.run(
          ToolFinder.of(moduleFinder, true, name),
          ToolCall.of("junit")
              .with("--select-module", name)
              .with("--reports-dir", paths.out("test-reports", "junit-" + name))
              .with(space.tweak(ToolCallTweak.WORKFLOW_TEST_JUNIT)),
          ToolRunner.RunModifier.RUN_WITH_PROVIDERS_CLASS_LOADER);
    }
  }
}
