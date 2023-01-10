package run.bach.tool;

import java.lang.invoke.MethodHandles;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import run.bach.ProjectOperator;
import run.bach.ProjectRunner;
import run.bach.external.ModulesLocators;
import run.bach.internal.ModulesSupport;
import run.duke.Duke;
import run.duke.ToolLogger;

public class LoadTool implements ProjectOperator {
  record Options(boolean __help, String what, String that, String... more) {
    List<String> thatAndMore() {
      return Stream.concat(Stream.of(that), Stream.of(more)).toList();
    }
  }

  public LoadTool() {}

  @Override
  public final String name() {
    return "load";
  }

  @Override
  public void run(ProjectRunner runner, ToolLogger logger, String... args) {
    var options = Duke.split(MethodHandles.lookup(), Options.class, args);
    if (options.__help()) {
      logger.log("Usage: %s <what> <that> <more...>".formatted(name()));
      return;
    }
    var browser = runner.browser();
    switch (options.what) {
      case "file" -> browser.load(URI.create(options.that), Path.of(options.more[0]));
      case "head" -> logger.log(browser.head(URI.create(options.that)));
      case "headers" -> {
        for (var entry : browser.head(URI.create(options.that)).headers().map().entrySet()) {
          logger.log(entry.getKey());
          for (var line : entry.getValue()) logger.log("  " + line);
        }
      }
      case "module" -> loadModule(runner, logger, options.thatAndMore());
      case "modules" -> loadModules(runner, logger, options.thatAndMore());
      case "text" -> logger.log(browser.read(URI.create(options.that)));
      default -> throw new IllegalArgumentException("Unknown load type: " + options.what);
    }
  }

  void loadModule(ProjectRunner runner, ToolLogger logger, List<String> modules) {
    var externalModules = runner.folders().externalModules();
    var locators =
        Stream.concat(
                runner.project().externals().locators().list().stream(),
                ModulesLocators.of(externalModules).list().stream())
            .toList();
    with_next_module:
    for (var module : modules) {
      if (ModuleFinder.of(externalModules).find(module).isPresent()) {
        logger.debug("Module %s is already present".formatted(module));
        continue; // with next module
      }
      for (var locator : locators) {
        var location = locator.locate(module);
        if (location == null) continue; // with next locator
        logger.debug("Module %s located via %s".formatted(module, locator.description()));
        var source = URI.create(location);
        var target = externalModules.resolve(module + ".jar");
        runner.browser().load(source, target); // "silent" load file ...
        continue with_next_module;
      }
      throw new RuntimeException("Module not locatable: " + module);
    }
  }

  void loadModules(ProjectRunner runner, ToolLogger logger, List<String> modules) {
    var externals = runner.folders().externalModules();
    var loaded = new TreeSet<String>();
    var difference = new TreeSet<String>();
    while (true) {
      var finders = List.of(ModuleFinder.of(externals)); // recreate in every loop
      var missing = ModulesSupport.listMissingNames(finders, Set.copyOf(modules));
      if (missing.isEmpty()) break;
      var size = missing.size();
      logger.debug("Load %d missing module%s".formatted(size, size == 1 ? "" : "s"));
      difference.retainAll(missing);
      if (!difference.isEmpty()) throw new Error("Still missing?! " + difference);
      difference.addAll(missing);
      loadModule(runner, logger, missing); // "silent" load module missing...
      loaded.addAll(missing);
      missing.forEach(logger::debug);
    }
    logger.log("Loaded %d module%s".formatted(loaded.size(), loaded.size() == 1 ? "" : "s"));
  }
}
