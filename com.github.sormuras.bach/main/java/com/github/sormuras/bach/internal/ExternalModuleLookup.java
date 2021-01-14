package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.ModuleLookup;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.util.Optional;

/**
 * A module lookup for an external module.
 *
 * @param module the name of the external module
 * @param uri the uri of the external module
 */
public record ExternalModuleLookup(String module, String uri) implements ModuleLookup {
  /**
   * @throws IllegalArgumentException if the given module is {@code null} or is not a legal name
   * @throws IllegalArgumentException if the given uri string violates RFC&nbsp;2396
   */
  public ExternalModuleLookup {
    ModuleDescriptor.newModule(module);
    URI.create(uri);
  }

  @Override
  public Optional<String> lookupModule(String module) {
    return this.module.equals(module) ? Optional.of(uri) : Optional.empty();
  }

  @Override
  public String toString() {
    return module + " -> " + uri;
  }
}
