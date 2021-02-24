package com.github.sormuras.bach.lookup;

import java.util.Optional;

/** Maps well-known JUnit Jupiter module names to their Maven Central artifacts. */
public class JUnitJupiterModuleLookup implements ModuleLookup {

  private final String version;

  /**
   * Constructs a new module searcher with the given version.
   *
   * @param version the version
   */
  public JUnitJupiterModuleLookup(String version) {
    this.version = version;
  }

  private Optional<String> uri(String suffix) {
    var artifact = "junit-jupiter" + (suffix.isEmpty() ? "" : '-' + suffix);
    return Optional.of(Maven.central("org.junit.jupiter", artifact, version));
  }

  @Override
  public LookupStability lookupStability() {
    return LookupStability.STABLE;
  }

  @Override
  public Optional<String> lookupUri(String module) {
    return switch (module) {
      case "org.junit.jupiter" -> uri("");
      case "org.junit.jupiter.api" -> uri("api");
      case "org.junit.jupiter.engine" -> uri("engine");
      case "org.junit.jupiter.migrationsupport" -> uri("migrationsupport");
      case "org.junit.jupiter.params" -> uri("params");
      default -> Optional.empty();
    };
  }

  @Override
  public String toString() {
    return "org.junit.jupiter[*] -> JUnit Jupiter " + version;
  }
}
