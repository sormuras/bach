package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.project.ModuleLookup;
import java.util.List;
import java.util.Optional;

/** A composable module lookup. */
public class ComposedModuleLookup implements ModuleLookup {

  private final List<ModuleLookup> lookups;

  /**
   * Initialize this composable module lookup with the given components.
   *
   * @param lookups the components that make up this composable module lookup
   */
  public ComposedModuleLookup(ModuleLookup... lookups) {
    this.lookups = List.of(lookups);
  }

  @Override
  public Optional<String> lookup(String module) {
    for (var lookup : lookups) {
      var optional = lookup.lookup(module);
      if (optional.isPresent()) return optional;
    }
    return Optional.empty();
  }
}
