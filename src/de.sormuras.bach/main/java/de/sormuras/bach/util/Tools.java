package de.sormuras.bach.util;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;

/** Tool registry. */
public class Tools {

  final Map<String, ToolProvider> map;

  public Tools() {
    this.map = new TreeMap<>();
    ServiceLoader.load(ToolProvider.class, ClassLoader.getSystemClassLoader()).stream()
        .map(ServiceLoader.Provider::get)
        .forEach(provider -> map.putIfAbsent(provider.name(), provider));
  }

  public ToolProvider get(String name) {
    var tool = map.get(name);
    if (tool == null) {
      throw new NoSuchElementException("No such tool: " + name);
    }
    return tool;
  }

  public void forEach(Consumer<ToolProvider> action) {
    map.values().forEach(action);
  }
}
