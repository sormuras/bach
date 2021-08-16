package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.ToolFinder;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;

public sealed interface ToolFinderSupport permits ConstantInterface {
  record ToolProviderToolFinder(List<ToolProvider> findAll) implements ToolFinder {}

  record CompositeToolFinder(List<ToolFinder> finders) implements ToolFinder {
    @Override
    public List<ToolProvider> findAll() {
      return finders.stream().flatMap(finder -> finder.findAll().stream()).toList();
    }

    @Override
    public Optional<ToolProvider> find(String name) {
      for (var finder : finders) {
        var tool = finder.find(name);
        if (tool.isPresent()) return tool;
      }
      return Optional.empty();
    }
  }

  record ModuleLayerToolFinder(ModuleLayer layer, ServiceLoader<ToolProvider> loader) implements ToolFinder {
    @Override
    public List<ToolProvider> findAll() {
      synchronized (loader) {
        return loader.stream()
            .filter(service -> service.type().getModule().getLayer() == layer)
            .map(ServiceLoader.Provider::get)
            .toList();
      }
    }
  }

  record ServiceLoaderToolFinder(ServiceLoader<ToolProvider> loader) implements ToolFinder {
    @Override
    public List<ToolProvider> findAll() {
      synchronized (loader) {
        return loader.stream().map(ServiceLoader.Provider::get).toList();
      }
    }
  }
}
