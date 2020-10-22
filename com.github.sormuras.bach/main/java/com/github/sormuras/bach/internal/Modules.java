package com.github.sormuras.bach.internal;

import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Module-related utilities. */
public class Modules {

  /**
   * Returns a new module finder with default search locations.
   *
   * @return a new module finder with default search locations
   */
  public static ModuleFinder finder() {
    var base = Path.of("");
    var workspace = base.resolve(".bach/workspace");
    var classes = workspace.resolve("classes/.build");
    var cache = base.resolve(".bach/cache");
    return ModuleFinder.of(classes, cache);
  }

  public static ModuleLayer layer(ModuleFinder finder) {
    var boot = ModuleLayer.boot();
    var configuration = boot.configuration().resolveAndBind(ModuleFinder.of(), finder, Set.of());
    var parent = Modules.class.getClassLoader();
    var controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), parent);
    return controller.layer();
  }

  private Modules() {}
}
