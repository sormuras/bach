package com.github.sormuras.bach;

import com.github.sormuras.bach.lookup.ExternalModuleLookup;
import com.github.sormuras.bach.lookup.JUnitJupiterModuleLookup;
import com.github.sormuras.bach.lookup.JUnitPlatformModuleLookup;
import com.github.sormuras.bach.lookup.Maven;
import com.github.sormuras.bach.lookup.ModuleLookup;
import java.util.Arrays;
import java.util.Optional;

/** A finder of external modules. */
public record Libraries(ModuleLookup... lookups) {

  public static Libraries of(ModuleLookup... lookups) {
    return new Libraries(lookups);
  }

  public boolean isEmpty() {
    return lookups.length == 0;
  }

  public record Found(String uri, ModuleLookup by) {}

  public Optional<Found> find(String name) {
    for (var lookup : lookups) {
      var uri = lookup.lookupUri(name);
      if (uri.isPresent()) return Optional.of(new Found(uri.get(), lookup));
    }
    return Optional.empty();
  }

  public Libraries with(ModuleLookup lookup, ModuleLookup... more) {
    var copy = Arrays.copyOf(lookups, lookups.length + 1 + more.length);
    copy[lookups.length] = lookup;
    if (more.length > 0) System.arraycopy(more, 0, copy, lookups.length + 1, more.length);
    return new Libraries(copy);
  }

  /** Link well-known JUnit modules to their Maven Central artifacts. */
  public enum JUnit implements ModuleLookup {

    /** Link modules of JUnit 5.7.0 to their Maven Central artifacts. */
    V_5_7_0("5.7.0", "1.7.0", "1.1.1", "1.2.0"),
    /** Link modules of JUnit 5.7.1 to their Maven Central artifacts. */
    V_5_7_1("5.7.1", "1.7.1", "1.1.1", "1.2.0");

    private final String version;
    private final Libraries libraries;

    JUnit(String jupiter, String platform, String apiguardian, String opentest4j) {
      this.version = jupiter;
      this.libraries =
          new Libraries(
              new JUnitJupiterModuleLookup(jupiter),
              new JUnitPlatformModuleLookup(platform),
              new ExternalModuleLookup(
                  "org.junit.vintage.engine",
                  Maven.central("org.junit.vintage", "junit-vintage-engine", jupiter)),
              new ExternalModuleLookup(
                  "org.apiguardian.api",
                  Maven.central("org.apiguardian", "apiguardian-api", apiguardian)),
              new ExternalModuleLookup(
                  "org.opentest4j", Maven.central("org.opentest4j", "opentest4j", opentest4j)));
    }

    @Override
    public Optional<String> lookupUri(String module) {
      return libraries.find(module).map(Libraries.Found::uri);
    }

    @Override
    public String toString() {
      return "JUnit " + version;
    }
  }
}
