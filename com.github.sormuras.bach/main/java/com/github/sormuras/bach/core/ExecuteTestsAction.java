package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.CommandResult;
import com.github.sormuras.bach.CommandResults;
import com.github.sormuras.bach.api.CodeSpace;
import java.lang.module.ModuleFinder;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;

public class ExecuteTestsAction extends BachAction {

  public ExecuteTestsAction(Bach bach) {
    super(bach);
  }

  public void execute() {
    var test = bach().project().spaces().test();
    var modules = test.modules();
    if (modules.isEmpty()) {
      bach().log("Test module list is empty, nothing to execute here.");
      return;
    }

    var tools = bach().project().tools();
    var testsEnabled = tools.enabled("test");
    var junitEnabled = tools.enabled("junit");
    var junitPresent = bach().findToolProvider("junit").isPresent();

    if (!testsEnabled && !junitEnabled) {
      bach().log("Test runs are disabled");
      return;
    }

    bach().log("Execute each test module");
    var results = new ArrayList<CommandResult>();
    for (var name : modules.toNames().toList()) {
      bach().say("Test module %s".formatted(name));
      // "test"
      var finder = buildTestModuleFinder(name);
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

    new CommandResults(results).requireSuccessful();
  }

  private ModuleFinder buildTestModuleFinder(String module) {
    var folders = bach().project().folders();
    var jar = folders.jar(CodeSpace.TEST, module, bach().project().version());
    return ModuleFinder.of(
        jar, // module under test
        folders.modules(CodeSpace.MAIN), // main modules
        folders.modules(CodeSpace.TEST), // (more) test modules
        folders.externals());
  }

  public CommandResult runTest(ToolProvider provider, List<String> arguments) {
    var providerClass = provider.getClass();
    var description = providerClass.getModule().getName() + "/" + providerClass.getName();
    return bach().run(provider, arguments, description);
  }

  public CommandResult runJUnit(ToolProvider provider, String module) {
    var project = bach().project();
    var tweaks = project.tools().tweaks();
    var junit =
        Command.of("junit")
            .with("--select-module", module)
            .withAll(tweaks.arguments(CodeSpace.TEST, "junit"))
            .withAll(tweaks.arguments(CodeSpace.TEST, "junit(" + module + ")"))
            .with("--reports-dir", project.folders().workspace("reports", "junit", module));

    var description = junit.toDescription(Commander.MAX_DESCRIPTION_LENGTH);
    return bach().run(provider, junit.arguments(), description);
  }
}
