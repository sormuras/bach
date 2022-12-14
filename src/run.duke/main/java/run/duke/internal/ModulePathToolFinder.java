package run.duke.internal;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import run.duke.Tool;
import run.duke.ToolFinder;
import run.duke.ToolOperator;

public record ModulePathToolFinder(Optional<String> description, Path... entries)
    implements ToolFinder {
  @Override
  public List<Tool> findTools() {
    var layer = defineModuleLayerForPathEntries();
    var finder =
        new ModuleLayerToolFinder(
            description,
            layer,
            Set.of(ToolFinder.class, ToolOperator.class, ToolProvider.class),
            module -> module.getLayer() == layer);
    return finder.findTools();
  }

  ModuleLayer defineModuleLayerForPathEntries() {
    var boot = ModuleLayer.boot();
    var finder = ModuleFinder.of(entries);
    var roots = streamAllModuleNames(finder).collect(Collectors.toSet());
    var configuration = boot.configuration().resolve(finder, ModuleFinder.of(), roots);
    var parentLoader = ClassLoader.getSystemClassLoader();
    return boot.defineModulesWithOneLoader(configuration, parentLoader);
  }

  Stream<String> streamAllModuleNames(ModuleFinder finder) {
    return finder.findAll().stream().map(ModuleReference::descriptor).map(ModuleDescriptor::name);
  }
}
