package com.github.sormuras.bach.lookup;

import java.util.List;
import java.util.Optional;

/** Lookup well-known JUnit modules published at Maven Central. */
record JUnitModuleLookup(String version, List<ModuleLookup> lookups) implements ModuleLookup {

  static ModuleLookup of(String jupiter, String platform, String apiguardian, String opentest4j) {
    var lookups =
        List.of(
            new JUnitJupiterModuleLookup(jupiter),
            new JUnitPlatformModuleLookup(platform),
            ModuleLookup.external("org.junit.vintage.engine")
                .viaMavenCentral("org.junit.vintage:junit-vintage-engine:" + jupiter),
            ModuleLookup.external("org.apiguardian.api")
                .viaMavenCentral("org.apiguardian:apiguardian-api:" + apiguardian),
            ModuleLookup.external("org.opentest4j")
                .viaMavenCentral("org.opentest4j:opentest4j:" + opentest4j));
    return new JUnitModuleLookup(jupiter, lookups);
  }

  @Override
  public Optional<String> lookupUri(String module) {
    for (var lookup : lookups) {
      var found = lookup.lookupUri(module);
      if (found.isPresent()) return found;
    }
    return Optional.empty();
  }

  @Override
  public String toString() {
    return "JUnit " + version;
  }
}
