package com.github.sormuras.bach;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

@FunctionalInterface
public interface ServiceFinder<S> {

  List<S> findAll();

  default String nameOf(S service) {
    return service.getClass().getSimpleName();
  }

  default Optional<S> find(String name) {
    return findAll().stream().filter(service -> nameOf(service).equals(name)).findFirst();
  }

  default List<S> list(String name) {
    return findAll().stream().filter(service -> nameOf(service).equals(name)).toList();
  }

  record ServiceLoaderServiceFinder<S>(ServiceLoader<S> loader) implements ServiceFinder<S> {
    @Override
    public List<S> findAll() {
      synchronized (loader) {
        return loader.stream().map(ServiceLoader.Provider::get).toList();
      }
    }
  }

  record ModuleLayerServiceFinder<S>(ModuleLayer layer, ServiceLoader<S> loader)
      implements ServiceFinder<S> {
    @Override
    public List<S> findAll() {
      synchronized (loader) {
        return loader.stream()
            .filter(service -> service.type().getModule().getLayer() == layer)
            .map(ServiceLoader.Provider::get)
            .toList();
      }
    }
  }
}
