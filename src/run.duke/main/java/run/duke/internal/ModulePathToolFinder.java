package run.duke.internal;

import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import run.duke.Tool;
import run.duke.ToolFinder;

public record ModulePathToolFinder(Path... entries) implements ToolFinder {
  @Override
  public List<Tool> tools() {
    var layer = defineModuleLayerForPathEntries();
    var finder = new ModuleLayerToolFinder(layer, module -> module.getLayer() == layer);
    return finder.tools();
  }

  ModuleLayer defineModuleLayerForPathEntries() {
    var boot = ModuleLayer.boot();
    var finder = ModuleFinder.of(entries);
    var roots = streamAllModuleNames(finder).collect(Collectors.toSet());
    try {
      var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), roots);
      var parentLoader = ClassLoader.getSystemClassLoader();
      return boot.defineModulesWithOneLoader(configuration, parentLoader);
    } catch (FindException exception) {
      return ModuleLayer.empty();
    }
  }

  Stream<String> streamAllModuleNames(ModuleFinder finder) {
    return finder.findAll().stream().map(ModuleReference::descriptor).map(ModuleDescriptor::name);
  }
}
