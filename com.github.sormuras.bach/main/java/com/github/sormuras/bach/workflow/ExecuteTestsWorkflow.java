package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Call;
import com.github.sormuras.bach.Workflow;
import com.github.sormuras.bach.internal.Durations;
import com.github.sormuras.bach.project.DeclaredModule;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingFile;

public class ExecuteTestsWorkflow extends Workflow {

  public ExecuteTestsWorkflow(Bach bach) {
    super(bach);
  }

  public void execute() {
    var test = bach.project().spaces().test();
    if (test.modules().isEmpty()) {
      bach.log("Test module list is empty, nothing to do here.");
      return;
    }
    var modules = DeclaredModuleFinder.of(test.modules());

    var folders = bach.folders();
    var externalsFinder = ModuleFinder.of(folders.externalModules());
    var junitPlatformJFR = externalsFinder.find("org.junit.platform.jfr");
    var start = Instant.now();
    if (junitPlatformJFR.isEmpty()) {
      execute(modules, true, true);
      var duration = Durations.beautifyBetweenNow(start);
      bach.log(Level.INFO, "Ran test(s) in %s".formatted(duration));
    } else {
      var file = folders.workspace("junit-platform.jfr");
      try (var recording = new Recording()) {
        recording.setDestination(file);
        recording.setToDisk(false);
        recording.start();
        execute(modules, true, true);
        bach.log("Wrote flight-recording to " + file.toUri());
      } catch (Exception exception) {
        bach.logbook().log(exception);
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
        bach.logbook().log(exception);
      }
      var duration = Durations.beautifyBetweenNow(start);
      bach.log(Level.INFO, "Ran %d test%s in %s".formatted(count, count == 1 ? "" : "s", duration));
    }
  }

  public void execute(DeclaredModuleFinder modules, boolean testsEnabled, boolean junitEnabled) {
    bach.log("Execute each test module");
    var junitPresent = bach.runner().findToolProvider("junit").isPresent();

    var runs = new ArrayList<Logbook.Run>();
    for (var module : modules.modules().toList()) {
      var name = module.name();
      bach.log(Level.INFO, "Execute tests in module %s".formatted(name));
      var finder = ModuleFinder.of(computeModulePaths(module).toArray(Path[]::new));
      // "test"
      if (testsEnabled)
        bach.runner()
            .streamToolProviders(finder, ModuleFinder.ofSystem(), true, name)
            .filter(provider -> provider.getClass().getModule().getName().equals(name))
            .filter(provider -> provider.name().equals("test"))
            .map(provider -> runTest(provider, List.of()))
            .forEach(runs::add);
      // "junit"
      if (junitEnabled && junitPresent)
        bach.runner()
            .streamToolProviders(finder, ModuleFinder.ofSystem(), true, name)
            .filter(provider -> provider.name().equals("junit"))
            .findFirst()
            .map(provider -> runJUnit(provider, name))
            .map(runs::add);
    }

    runs.forEach(Logbook.Run::requireSuccessful);
  }

  public List<Path> computeModulePaths(DeclaredModule module) {
    var descriptor = module.descriptor();
    var name = descriptor.name();
    var version = descriptor.version().orElse(project.version().value());
    var jar = name + "@" + version + "-test.jar";
    return List.of(
        bach.folders().workspace("modules-test", jar), // module under test
        bach.folders().workspace("modules"), // main modules
        bach.folders().workspace("modules-test"), // (more) test modules
        bach.folders().externalModules());
  }

  public Logbook.Run runTest(ToolProvider provider, List<String> arguments) {
    var providerClass = provider.getClass();
    var description = providerClass.getModule().getName() + "/" + providerClass.getName();
    bach.log(Level.INFO, "  %-9s %s", "test", description);
    return bach.runner().run(provider, arguments);
  }

  public Logbook.Run runJUnit(ToolProvider provider, String module) {
    var junit =
        Call.tool("junit")
            .with("--select-module", module)
            .with("--reports-dir", bach.folders().workspace("reports", "junit", module));
    bach.log(Level.INFO, "  %-9s %s", provider.name(), junit.toDescription(117));
    return bach.runner().run(provider, junit.arguments());
  }
}
