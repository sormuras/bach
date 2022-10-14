package run.bach.internal.tool;

import java.lang.module.ModuleFinder;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import run.bach.Bach;
import run.bach.ToolOperator;
import run.bach.internal.ModulesSupport;

public record LoadModulesOperator(String name) implements ToolOperator {
  public LoadModulesOperator() {
    this("load-modules");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    // if (help(bach, arguments, "<more-missing-module-names...>")) return;
    var externals = bach.paths().externalModules();
    var loaded = new TreeSet<String>();
    var difference = new TreeSet<String>();
    while (true) {
      var finders = List.of(ModuleFinder.of(externals)); // recreate in every loop
      var missing = ModulesSupport.listMissingNames(finders, Set.copyOf(arguments));
      if (missing.isEmpty()) break;
      bach.debug(
          "Load %d missing module%s".formatted(missing.size(), missing.size() == 1 ? "" : "s"));
      difference.retainAll(missing);
      if (!difference.isEmpty()) throw new Error("Still missing?! " + difference);
      difference.addAll(missing);
      bach.run("load-module", missing);
      loaded.addAll(missing);
    }
    bach.debug("Loaded %d module%s".formatted(loaded.size(), loaded.size() == 1 ? "" : "s"));
  }
}
