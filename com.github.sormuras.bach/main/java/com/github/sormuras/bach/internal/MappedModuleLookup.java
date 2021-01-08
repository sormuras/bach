package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.ModuleLookup;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

/**
 * A module lookup backed by a map.
 *
 * @param map the map
 */
public record MappedModuleLookup(Map<String, String> map) implements ModuleLookup {

  /**
   * @throws IllegalArgumentException if the given map contains not legal module name keys or values
   *     that violate RFC&nbsp;2396
   */
  public MappedModuleLookup {
    for (var entry : map.entrySet()) {
      ModuleDescriptor.newModule(entry.getKey());
      URI.create(entry.getValue());
    }
  }

  @Override
  public Optional<String> lookupModule(String module) {
    return Optional.ofNullable(map.get(module));
  }
}
