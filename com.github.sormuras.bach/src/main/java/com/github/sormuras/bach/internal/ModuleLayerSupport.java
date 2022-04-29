package com.github.sormuras.bach.internal;

import static java.lang.ModuleLayer.defineModulesWithOneLoader;
import static java.lang.module.Configuration.resolveAndBind;

import java.lang.module.ModuleFinder;
import java.util.List;
import java.util.Set;

public interface ModuleLayerSupport {
  static ModuleLayer layer(ModuleFinder finder, boolean assertions, String... roots) {
    var parentClassLoader = ModuleLayerSupport.class.getClassLoader();
    var parentModuleLayer = ModuleLayer.boot();
    var parents = List.of(parentModuleLayer.configuration());
    var configuration = resolveAndBind(ModuleFinder.of(), parents, finder, Set.of(roots));
    var layers = List.of(parentModuleLayer);
    var controller = defineModulesWithOneLoader(configuration, layers, parentClassLoader);
    var layer = controller.layer();
    if (assertions) for (var root : roots) layer.findLoader(root).setDefaultAssertionStatus(true);
    return layer;
  }
}
