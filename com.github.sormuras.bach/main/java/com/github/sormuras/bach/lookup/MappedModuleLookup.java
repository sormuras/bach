package com.github.sormuras.bach.lookup;

import java.util.Map;
import java.util.Optional;

/**
 * A stable module lookup for a set of external modules.
 *
 * @param uris the uri mapping
 */
public record MappedModuleLookup(Map<String, String> uris) implements ModuleLookup {
  /**
   * @throws IllegalArgumentException if the given map is {@code null}
   * @throws IllegalArgumentException if a module key is {@code null} or is not a legal name
   * @throws IllegalArgumentException if a uri value string violates RFC&nbsp;2396
   */
  public MappedModuleLookup {
    uris.forEach(ExternalModuleLookup::new);
  }

  @Override
  public LookupStability lookupStability() {
    return LookupStability.STABLE;
  }

  @Override
  public Optional<String> lookupUri(String module) {
    return Optional.ofNullable(uris.get(module));
  }

  @Override
  public String toString() {
    return "MappedModuleLookup with " + uris.size() + " entries";
  }
}
