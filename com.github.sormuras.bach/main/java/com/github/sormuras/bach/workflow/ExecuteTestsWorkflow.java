package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolRun;
import com.github.sormuras.bach.ToolRuns;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.api.DeclaredModuleFinder;
import com.github.sormuras.bach.internal.Strings;
import com.github.sormuras.bach.tool.AnyCall;
import com.github.sormuras.bach.trait.ToolTrait;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingFile;

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

    if (!testsEnabled && !junitEnabled) {
      bach().log("Test runs are disabled, nothing to do here.");
      return;
    }

    var folders = bach().project().folders();
    var externalsFinder = ModuleFinder.of(folders.externalModules());
    var junitPlatformJFR = externalsFinder.find("org.junit.platform.jfr");
    var start = Instant.now();
    if (junitPlatformJFR.isEmpty()) {
      execute(modules, testsEnabled, junitEnabled);
      var duration = Strings.toString(Duration.between(start, Instant.now()));
      bach().say("Ran test(s) in %s".formatted(duration));
    } else {
      var file = folders.workspace("junit-platform.jfr");
      try (var recording = new Recording()) {
        recording.setDestination(file);
        recording.setToDisk(false);
        recording.start();
        execute(modules, testsEnabled, junitEnabled);
        bach().say("Wrote flight-recording to " + file.toUri());
      } catch (Exception exception) {
        bach().logbook().log(exception);
      }
      var count = 0;
      try {
        var events = RecordingFile.readAllEvents(file);
        for (var event : events) {
          if ("org.junit.TestExecution".equals(event.getEventType().getName())) {
            count++;
          }
        }
      } catch (Exception exception) {
        bach().logbook().log(exception);
      }
      var duration = Strings.toString(Duration.between(start, Instant.now()));
      bach().say("Ran %d test%s in %s".formatted(count, count == 1 ? "" : "s", duration));
    }
  }

  protected void execute(DeclaredModuleFinder modules, boolean testsEnabled, boolean junitEnabled) {
    bach().log("Execute each test module");
    var junitPresent = bach().findToolProvider("junit").isPresent();

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
            .findFirst()
            .map(provider -> runJUnit(provider, name))
            .map(results::add);
    }

    new ToolRuns(results).requireSuccessful();
  }

  protected List<Path> computeModulePaths(String module) {
    var folders = bach().project().folders();
    return List.of(
        folders.jar(CodeSpace.TEST, module, bach().project().version()), // module under test
        folders.modules(CodeSpace.MAIN), // main modules
        folders.modules(CodeSpace.TEST), // (more) test modules
        folders.externalModules());
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
