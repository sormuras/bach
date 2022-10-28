package run.bach.toolfinder;

import java.util.List;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import run.bach.Tool;
import run.bach.ToolFinder;

public record ServiceLoaderToolFinder(ModuleLayer layer, ServiceLoader<ToolProvider> loader)
    implements ToolFinder {
  @Override
  public List<Tool> findAll() {
    synchronized (loader) {
      return loader.stream()
          .filter(service -> service.type().getModule().getLayer() == layer)
          .map(ServiceLoader.Provider::get)
          .map(Tool::ofToolProvider)
          .toList();
    }
  }
}
