package run.bach.tool;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import run.bach.Bach;
import run.bach.external.ModulesLocators;
import run.bach.internal.ModulesSupport;
import run.duke.CommandLineInterface;

public class LoadTool implements Bach.Operator {
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
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var options = CommandLineInterface.of(MethodHandles.lookup(), Options.class).split(args);
    if (options.__help()) {
      out.println("Usage: %s <what> <that> <more...>".formatted(name()));
      return 0;
    }
    var browser = bach.browser();
    switch (options.what) {
      case "file" -> browser.load(URI.create(options.that), Path.of(options.more[0]));
      case "head" -> out.println(browser.head(URI.create(options.that)));
      case "headers" -> {
        for (var entry : browser.head(URI.create(options.that)).headers().map().entrySet()) {
          out.println(entry.getKey());
          for (var line : entry.getValue()) out.println("  " + line);
        }
      }
      case "module" -> loadModule(bach, options.thatAndMore());
      case "modules" -> loadModules(bach, out, options.thatAndMore());
      case "text" -> out.println(browser.read(URI.create(options.that)));
      default -> {
        err.println("Unknown load type: " + options.what);
        return 1;
      }
    }
    return 0;
  }

  void loadModule(Bach bach, List<String> modules) {
    var externalModules = bach.folders().externalModules();
    var locators =
        Stream.concat(
                bach.project().externals().locators().list().stream(),
                ModulesLocators.of(externalModules).list().stream())
            .toList();
    with_next_module:
    for (var module : modules) {
      if (ModuleFinder.of(externalModules).find(module).isPresent()) {
        // TODO debug("Module %s is already present".formatted(module));
        continue; // with next module
      }
      for (var locator : locators) {
        var location = locator.locate(module);
        if (location == null) continue; // with next locator
        // TODO debug("Module %s located via %s".formatted(module, locator.description()));
        var source = URI.create(location);
        var target = externalModules.resolve(module + ".jar");
        bach.browser().load(source, target); // "silent" load file ...
        continue with_next_module;
      }
      throw new RuntimeException("Module not locatable: " + module);
    }
  }

  void loadModules(Bach bach, PrintWriter out, List<String> modules) {
    var externals = bach.folders().externalModules();
    var loaded = new TreeSet<String>();
    var difference = new TreeSet<String>();
    while (true) {
      var finders = List.of(ModuleFinder.of(externals)); // recreate in every loop
      var missing = ModulesSupport.listMissingNames(finders, Set.copyOf(modules));
      if (missing.isEmpty()) break;
      var size = missing.size();
      // TODO debug("Load %d missing module%s".formatted(size, size == 1 ? "" : "s"));
      difference.retainAll(missing);
      if (!difference.isEmpty()) throw new Error("Still missing?! " + difference);
      difference.addAll(missing);
      loadModule(bach, missing); // "silent" load module missing...
      loaded.addAll(missing);
      // TODO missing.forEach(this::debug);
    }
    out.println("Loaded %d module%s".formatted(loaded.size(), loaded.size() == 1 ? "" : "s"));
  }
}
