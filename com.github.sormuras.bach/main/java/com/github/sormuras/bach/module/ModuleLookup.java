package com.github.sormuras.bach.module;

import java.net.URI;
import java.util.Optional;

/** Find an optional URI for a specific module name. */
@FunctionalInterface
public interface ModuleLookup {
  /**
   * Returns an optional URI of the given module name.
   *
   * @param module the name of the module to find
   * @return a URI wrapped in an optional
   */
  Optional<URI> find(String module);
}
