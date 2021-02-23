package com.github.sormuras.bach.project;

import com.github.sormuras.bach.lookup.ModuleLookup;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * An external module manager.
 *
 * @param requires modules on which the current project has a dependence
 * @param lookups sequence of module lookup objects to query for uris
 */
public record Libraries(Set<String> requires, List<ModuleLookup> lookups) {

  public static Libraries of(ModuleLookup... lookups) {
    return new Libraries(Set.of(), List.of(lookups));
  }

  public record Found(String uri, ModuleLookup by) {}

  public Optional<Found> find(String module) {
    for (var lookup : lookups) {
      var uri = lookup.lookupUri(module);
      if (uri.isPresent()) return Optional.of(new Found(uri.get(), lookup));
    }
    return Optional.empty();
  }

  public Libraries withRequires(String module) {
    var requires = new HashSet<>(this.requires);
    requires.add(module);
    return new Libraries(Set.copyOf(requires), lookups);
  }

  public Libraries withModuleLookup(ModuleLookup lookup) {
    var lookups = new ArrayList<>(this.lookups);
    lookups.add(lookup);
    return new Libraries(requires, List.copyOf(lookups));
  }
}
