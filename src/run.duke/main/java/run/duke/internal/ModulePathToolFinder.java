package run.duke.internal;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import run.duke.Tool;
import run.duke.ToolFinder;
import run.duke.ToolRunner;

public record ModulePathToolFinder(String description, Path... entries) implements ToolFinder {
  @Override
  public List<String> identifiers() {
    return tools().map(Tool::identifier).toList();
  }

  @Override
  public Optional<Tool> find(String string, ToolRunner runner) {
    return tools().filter(tool -> tool.test(string)).findFirst();
  }

  Stream<Tool> tools() {
    var boot = ModuleLayer.boot();
    var finder = ModuleFinder.of(entries);
    var roots = streamAllModuleNames(finder).collect(Collectors.toSet());
    var configuration = boot.configuration().resolve(finder, ModuleFinder.of(), roots);
    var parentLoader = ClassLoader.getSystemClassLoader();
    var layer = boot.defineModulesWithOneLoader(configuration, parentLoader);
    var loader = ServiceLoader.load(layer, ToolProvider.class);
    return loader.stream()
        .filter(service -> service.type().getModule().getLayer() == layer)
        .map(ServiceLoader.Provider::get)
        .map(Tool::new);
  }

  Stream<String> streamAllModuleNames(ModuleFinder finder) {
    return finder.findAll().stream().map(ModuleReference::descriptor).map(ModuleDescriptor::name);
  }
}
