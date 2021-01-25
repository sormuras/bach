package com.github.sormuras.bach.lookup;

import java.util.Optional;

/** Maps well-known JUnit Jupiter module names to their Maven Central artifacts. */
public class JUnitJupiter implements ModuleLookup {

  private final String version;

  /**
   * Constructs a new module searcher with the given version.
   *
   * @param version the version
   */
  public JUnitJupiter(String version) {
    this.version = version;
  }

  private Optional<String> map(String suffix) {
    var artifact = "junit-jupiter" + (suffix.isEmpty() ? "" : '-' + suffix);
    return Optional.of(Maven.central("org.junit.jupiter", artifact, version));
  }

  @Override
  public Optional<String> lookupModule(String module) {
    return switch (module) {
      case "org.junit.jupiter" -> map("");
      case "org.junit.jupiter.api" -> map("api");
      case "org.junit.jupiter.engine" -> map("engine");
      case "org.junit.jupiter.migrationsupport" -> map("migrationsupport");
      case "org.junit.jupiter.params" -> map("params");
      default -> Optional.empty();
    };
  }

  @Override
  public String toString() {
    return "org.junit.jupiter[*] -> JUnit Jupiter " + version;
  }
}
