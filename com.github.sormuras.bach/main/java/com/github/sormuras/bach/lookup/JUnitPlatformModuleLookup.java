package com.github.sormuras.bach.lookup;

import java.util.Optional;

/** Maps well-known JUnit Platform module names to their Maven Central artifacts. */
public class JUnitPlatformModuleLookup implements ModuleLookup {

  private final String version;

  /**
   * Constructs a new module searcher with the given version.
   *
   * @param version the version
   */
  public JUnitPlatformModuleLookup(String version) {
    this.version = version;
  }

  private Optional<String> uri(String suffix) {
    var artifact = "junit-platform-" + suffix.replace('.', '-');
    return Optional.of(Maven.central("org.junit.platform", artifact, version));
  }

  @Override
  public Optional<String> lookupUri(String module) {
    return switch (module) {
      case "org.junit.platform.commons" -> uri("commons");
      case "org.junit.platform.console" -> uri("console");
      case "org.junit.platform.engine" -> uri("engine");
      case "org.junit.platform.jfr" -> uri("jfr");
      case "org.junit.platform.launcher" -> uri("launcher");
      case "org.junit.platform.reporting" -> uri("reporting");
      case "org.junit.platform.suite.api" -> uri("suite.api");
      case "org.junit.platform.testkit" -> uri("testkit");
      default -> Optional.empty();
    };
  }

  @Override
  public String toString() {
    return "org.junit.platform.[*] -> JUnit Platform " + version;
  }
}
