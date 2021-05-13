package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolRun;
import com.github.sormuras.bach.ToolRuns;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.tool.AnyCall;
import com.github.sormuras.bach.trait.ToolTrait;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;

public class ExecuteTestsWorkflow extends BachWorkflow {

  public ExecuteTestsWorkflow(Bach bach) {
    super(bach);
  }

  public void execute() {
    var test = bach().project().spaces().test();
    var modules = test.modules();
    if (modules.isEmpty()) {
      bach().log("Test module list is empty, nothing to do here.");
      return;
    }

    var tools = bach().project().tools();
    var testsEnabled = tools.enabled("test");
    var junitEnabled = tools.enabled("junit");
    var junitPresent = bach().findToolProvider("junit").isPresent();

    if (!testsEnabled && !junitEnabled) {
      bach().log("Test runs are disabled, nothing to do here.");
      return;
    }

    bach().log("Execute each test module");
    var results = new ArrayList<ToolRun>();
    for (var name : modules.toNames().toList()) {
      bach().say("Test module %s".formatted(name));
      var finder = ModuleFinder.of(computeModulePaths(name).toArray(Path[]::new));
      // "test"
      if (testsEnabled)
        bach()
            .streamToolProviders(finder, ModuleFinder.ofSystem(), true, name)
            .filter(provider -> provider.getClass().getModule().getName().equals(name))
            .filter(provider -> provider.name().equals("test"))
            .map(provider -> runTest(provider, List.of()))
            .forEach(results::add);
      // "junit"
      if (junitEnabled && junitPresent)
        bach()
          .streamToolProviders(finder, ModuleFinder.ofSystem(), true, name)
          .filter(provider -> provider.name().equals("junit"))
          .map(provider -> runJUnit(provider, name))
          .forEach(results::add);
    }

    new ToolRuns(results).requireSuccessful();
  }

  protected List<Path> computeModulePaths(String module) {
    var folders = bach().project().folders();
    return List.of(
        folders.jar(CodeSpace.TEST, module, bach().project().version()), // module under test
        folders.modules(CodeSpace.MAIN), // main modules
        folders.modules(CodeSpace.TEST), // (more) test modules
        folders.externals());
  }

  protected ToolRun runTest(ToolProvider provider, List<String> arguments) {
    var providerClass = provider.getClass();
    var description = providerClass.getModule().getName() + "/" + providerClass.getName();
    return bach().run(provider, arguments, description);
  }

  protected ToolRun runJUnit(ToolProvider provider, String module) {
    var project = bach().project();
    var tweaks = project.tools().tweaks();
    var junit =
        new AnyCall("junit")
            .with("--select-module", module)
            .withAll(tweaks.arguments(CodeSpace.TEST, "junit"))
            .withAll(tweaks.arguments(CodeSpace.TEST, "junit(" + module + ")"))
            .with("--reports-dir", project.folders().workspace("reports", "junit", module));

    var description = junit.toDescription(ToolTrait.MAX_DESCRIPTION_LENGTH);
    return bach().run(provider, junit.arguments(), description);
  }
}
