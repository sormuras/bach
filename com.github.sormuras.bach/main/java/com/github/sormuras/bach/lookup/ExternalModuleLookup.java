package com.github.sormuras.bach.lookup;

import java.util.Optional;

/**
 * A stable module lookup for an external module.
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
    ModuleLookup.requireValidModuleName(module);
    ModuleLookup.requireValidUri(uri);
  }

  @Override
  public LookupStability lookupStability() {
    return LookupStability.STABLE;
  }

  @Override
  public Optional<String> lookupUri(String module) {
    return this.module.equals(module) ? Optional.of(uri) : Optional.empty();
  }
}
