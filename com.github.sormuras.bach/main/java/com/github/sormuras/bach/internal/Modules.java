package com.github.sormuras.bach.internal;

import java.lang.module.ModuleFinder;
import java.util.List;
import java.util.Set;

/** Module-related utilities. */
public class Modules {

  public static ModuleLayer layer(ModuleFinder finder) {
    var boot = ModuleLayer.boot();
    var configuration = boot.configuration().resolveAndBind(ModuleFinder.of(), finder, Set.of());
    var parent = Modules.class.getClassLoader();
    var controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), parent);
    return controller.layer();
  }

  /** Hide default constructor. */
  private Modules() {}
}
