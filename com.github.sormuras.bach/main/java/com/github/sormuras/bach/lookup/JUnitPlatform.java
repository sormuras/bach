package com.github.sormuras.bach.lookup;

import java.util.Optional;

/** Maps well-known JUnit Platform module names to their Maven Central artifacts. */
public class JUnitPlatform implements ModuleLookup {

  private final String version;

  /**
   * Constructs a new module searcher with the given version.
   *
   * @param version the version
   */
  public JUnitPlatform(String version) {
    this.version = version;
  }

  private Optional<String> map(String suffix) {
    var artifact = "junit-platform-" + suffix.replace('.', '-');
    return Optional.of(Maven.central("org.junit.platform", artifact, version));
  }

  @Override
  public Optional<String> lookupModule(String module) {
    return switch (module) {
      case "org.junit.platform.commons" -> map("commons");
      case "org.junit.platform.console" -> map("console");
      case "org.junit.platform.engine" -> map("engine");
      case "org.junit.platform.jfr" -> map("jfr");
      case "org.junit.platform.launcher" -> map("launcher");
      case "org.junit.platform.reporting" -> map("reporting");
      case "org.junit.platform.suite.api" -> map("suite.api");
      case "org.junit.platform.testkit" -> map("testkit");
      default -> Optional.empty();
    };
  }

  @Override
  public String toString() {
    return "org.junit.platform.[*] -> JUnit Platform " + version;
  }
}
