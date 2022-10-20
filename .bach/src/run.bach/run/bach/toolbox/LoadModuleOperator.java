package run.bach.toolbox;

import java.lang.module.ModuleFinder;
import java.net.URI;
import java.util.List;
import run.bach.Bach;
import run.bach.ToolOperator;

public record LoadModuleOperator(String name) implements ToolOperator {
  public LoadModuleOperator() {
    this("load-module");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    // if (help(bach, arguments, "<module-names...>")) return;
    var externals = bach.paths().externalModules();
    with_next_module:
    for (var module : arguments) {
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
        bach.browser().load(source, target);
        continue with_next_module;
      }
      throw new RuntimeException("Module not locatable: " + module);
    }
  }
}
