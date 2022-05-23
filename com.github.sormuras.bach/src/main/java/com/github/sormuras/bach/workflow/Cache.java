package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolOperator;
import com.github.sormuras.bach.internal.ModulesSupport;
import java.io.PrintWriter;
import java.lang.module.ModuleFinder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Cache implements ToolOperator {

  static final String NAME = "cache";

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    cacheAllExternalTools(bach);
    cacheExternalModules(bach, bach.project().externals().requires());
    cacheExternalModules(
        bach,
        ModulesSupport.listMissingModules(
            List.of(bach.project().spaces().toModuleFinder()), Set.of()));
    cacheMissingExternalModules(bach);
    return 0;
  }

  void cacheAllExternalTools(Bach bach) {
    var calls = new ArrayList<ToolCall>();
    for (var external : bach.project().externals().tools()) {
      var directory = bach.configuration().paths().externalTools(external.name());
      if (external.from().isPresent()) {
        var source = external.from().get();
        var begin = source.lastIndexOf('/') + 1;
        var end = source.indexOf('#', begin);
        var name = source.substring(begin, end != -1 ? end : source.length());
        var target = directory.resolve(name);
        calls.add(ToolCall.of("load-and-verify", target, source));
      }
      for (var asset : external.assets()) {
        var target = directory.resolve(asset.name());
        var source = asset.from();
        calls.add(ToolCall.of("load-and-verify", target, source));
      }
    }
    calls.forEach(bach::run);
  }

  void cacheExternalModules(Bach bach, Collection<String> modules) {
    if (modules.isEmpty()) return;
    var printer = bach.configuration().printer();
    var verbose = bach.configuration().isVerbose();
    var externals = bach.configuration().paths().externalModules();
    var finder = ModuleFinder.of(externals);
    module_loop:
    for (var module : modules) {
      if (finder.find(name()).isPresent()) continue;
      for (var locator : bach.project().externals().locators()) {
        var location = locator.find(module);
        if (location.isEmpty()) continue;
        if (verbose) {
          printer.out("Located module `%s` via %s".formatted(module, locator.caption()));
        }
        bach.run("load-and-verify", externals.resolve(module + ".jar"), location.get());
        continue module_loop;
      }
      throw new RuntimeException("Can not locate module: " + module);
    }
  }

  void cacheMissingExternalModules(Bach bach) {
    var printer = bach.configuration().printer();
    var verbose = bach.configuration().isVerbose();
    var externals = bach.configuration().paths().externalModules();
    var loaded = new TreeSet<String>();
    var difference = new TreeSet<String>();
    while (true) {
      var finders = List.of(ModuleFinder.of(externals)); // recreate in every loop
      var missing = ModulesSupport.listMissingModules(finders, Set.of());
      if (missing.isEmpty()) break;
      if (verbose) {
        printer.out(
            "Grab %d missing external module%s"
                .formatted(missing.size(), missing.size() == 1 ? "" : "s"));
      }
      difference.retainAll(missing);
      if (!difference.isEmpty()) throw new Error("Still missing?! " + difference);
      difference.addAll(missing);
      cacheExternalModules(bach, missing);
      loaded.addAll(missing);
    }
    if (verbose) {
      printer.out("Grabbed %d module%s".formatted(loaded.size(), loaded.size() == 1 ? "" : "s"));
    }
  }
}
