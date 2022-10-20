package run.bach.toolbox;

import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import run.bach.Bach;
import run.bach.ToolOperator;
import run.bach.internal.ModulesSupport;

public record LoadTool(String name) implements ToolOperator {
  public LoadTool() {
    this("load");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    if (arguments.isEmpty()) {
      bach.info("Usage: %s <operation> <arg> [args...]".formatted(name()));
      return;
    }
    var browser = bach.browser();
    var deque = new ArrayDeque<>(arguments);
    var operation = deque.pop();
    switch (operation) {
      case "file" -> browser.load(URI.create(deque.pop()), Path.of(deque.pop()));
      case "head" -> bach.info(browser.head(URI.create(deque.pop())));
      case "headers" -> {
        for (var entry : browser.head(URI.create(deque.pop())).headers().map().entrySet()) {
          bach.info(entry.getKey());
          for (var line : entry.getValue()) bach.debug("  " + line);
        }
      }
      case "module" -> loadModule(bach, deque.stream().toList());
      case "modules" -> loadModules(bach, deque.stream().toList());
      case "text" -> bach.info(browser.read(URI.create(deque.pop())));
      default -> throw new UnsupportedOperationException(operation);
    }
  }

  void loadModule(Bach bach, List<String> modules) {
    var externals = bach.paths().externalModules();
    with_next_module:
    for (var module : modules) {
      if (ModuleFinder.of(externals).find(module).isPresent()) {
        bach.debug("Module %s is already present".formatted(module));
        continue; // with next module
      }
      for (var locator : bach.locators().list()) {
        var location = locator.locate(module);
        if (location == null) continue; // with next locator
        bach.debug("Module %s located via %s".formatted(module, locator.description()));
        var source = URI.create(location);
        var target = externals.resolve(module + ".jar");
        bach.browser().load(source, target); // "silent" load file ...
        continue with_next_module;
      }
      throw new RuntimeException("Module not locatable: " + module);
    }
  }

  void loadModules(Bach bach, List<String> modules) {
    var externals = bach.paths().externalModules();
    var loaded = new TreeSet<String>();
    var difference = new TreeSet<String>();
    while (true) {
      var finders = List.of(ModuleFinder.of(externals)); // recreate in every loop
      var missing = ModulesSupport.listMissingNames(finders, Set.copyOf(modules));
      if (missing.isEmpty()) break;
      var size = missing.size();
      bach.debug("Load %d missing module%s".formatted(size, size == 1 ? "" : "s"));
      difference.retainAll(missing);
      if (!difference.isEmpty()) throw new Error("Still missing?! " + difference);
      difference.addAll(missing);
      loadModule(bach, missing); // "silent" load module missing...
      loaded.addAll(missing);
    }
    bach.debug("Loaded %d module%s".formatted(loaded.size(), loaded.size() == 1 ? "" : "s"));
  }
}
