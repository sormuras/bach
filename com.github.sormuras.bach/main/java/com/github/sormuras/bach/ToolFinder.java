package com.github.sormuras.bach;

import static java.lang.ModuleLayer.defineModulesWithOneLoader;
import static java.lang.module.Configuration.resolveAndBind;

import com.github.sormuras.bach.internal.DirectoriesToolProvider;
import com.github.sormuras.bach.internal.ExecuteProcessToolProvider;
import com.github.sormuras.bach.internal.GrabToolProvider;
import com.github.sormuras.bach.internal.ToolFinderSupport;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.spi.ToolProvider;

@FunctionalInterface
public interface ToolFinder {

  List<ToolProvider> findAll();

  default Optional<ToolProvider> find(String name) {
    return findAll().stream().filter(provider -> provider.name().equals(name)).findFirst();
  }

  static ToolFinder of(ToolProvider... providers) {
    return new ToolFinderSupport.ToolProviderToolFinder(List.of(providers));
  }

  static ToolFinder of(ModuleFinder finder, boolean assertions, String... roots) {
    var parentClassLoader = ClassLoader.getPlatformClassLoader();
    var parentModuleLayer = ModuleLayer.boot();
    var parents = List.of(parentModuleLayer.configuration());
    var configuration = resolveAndBind(ModuleFinder.of(), parents, finder, Set.of(roots));
    var layers = List.of(parentModuleLayer);
    var controller = defineModulesWithOneLoader(configuration, layers, parentClassLoader);
    var layer = controller.layer();
    if (assertions) for (var root : roots) layer.findLoader(root).setDefaultAssertionStatus(true);
    return ToolFinder.of(layer);
  }

  static ToolFinder of(ModuleLayer layer) {
    var loader = ServiceLoader.load(layer, ToolProvider.class);
    return new ToolFinderSupport.ModuleLayerToolFinder(layer, loader);
  }

  static ToolFinder of(ClassLoader loader) {
    return ToolFinder.of(ServiceLoader.load(ToolProvider.class, loader));
  }

  static ToolFinder of(ServiceLoader<ToolProvider> loader) {
    return new ToolFinderSupport.ServiceLoaderToolFinder(loader);
  }

  static ToolFinder ofPrograms(Path directory, Path java, String argsfile) {
    return new ToolFinderSupport.ProgramsToolFinder(directory, java, argsfile);
  }

  static ToolFinder ofLayers(Path directory) {
    return new ToolFinderSupport.LayersToolFinder(directory);
  }

  static ToolFinder ofBach() {
    return ToolFinder.of(
        new DirectoriesToolProvider(), new ExecuteProcessToolProvider(), new GrabToolProvider());
  }

  static ToolFinder ofSystem() {
    return ToolFinder.of(ClassLoader.getSystemClassLoader());
  }

  static ToolFinder compose(ToolFinder... finders) {
    return new ToolFinderSupport.CompositeToolFinder(List.of(finders));
  }
}
