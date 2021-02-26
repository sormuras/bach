package com.github.sormuras.bach.project;

import com.github.sormuras.bach.lookup.ModuleLookup;
import com.github.sormuras.bach.lookup.ModuleMetadata;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * An external module manager.
 *
 * @param requires modules on which the current project has a dependence
 * @param lookups sequence of module lookup objects to query for uris
 * @param metamap metadata information about external modules
 */
public record Libraries(
    Set<String> requires, List<ModuleLookup> lookups, Map<String, ModuleMetadata> metamap) {

  public static Libraries of(ModuleLookup... lookups) {
    return new Libraries(Set.of(), List.of(lookups), Map.of());
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
    return new Libraries(Set.copyOf(requires), lookups, metamap);
  }

  public Libraries with(ModuleLookup lookup) {
    var lookups = new ArrayList<>(this.lookups);
    lookups.add(lookup);
    return new Libraries(requires, List.copyOf(lookups), metamap);
  }

  public Libraries with(ModuleMetadata metadata) {
    var metamap = new HashMap<>(this.metamap);
    metamap.put(metadata.name(), metadata);
    return new Libraries(requires, lookups, Map.copyOf(metamap));
  }
}
