package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.ModuleLookup;
import java.util.List;
import java.util.Optional;

/**
 * A composable module lookup.
 *
 *  @param lookups the components that make up this composable module lookup
 */
public record ComposedModuleLookup(List<ModuleLookup> lookups) implements ModuleLookup {

  @Override
  public Optional<String> lookupModule(String module) {
    for (var lookup : lookups) {
      var optional = lookup.lookupModule(module);
      if (optional.isPresent()) return optional;
    }
    return Optional.empty();
  }

  @Override
  public String toString() {
    return "ComposedModuleLookup (" + lookups.size() + ")";
  }
}
