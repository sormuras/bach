package com.github.sormuras.bach.external;

import static com.github.sormuras.bach.external.Maven.central;

import com.github.sormuras.bach.ExternalModuleLocator;

/** Locates "JUnit" modules via their Maven Central artifacts. */
public record JUnit(String version) implements ExternalModuleLocator {

  /**
   * Constructs a new JUnit module locator with the given version.
   *
   * @param version the JUnit version
   */
  public static JUnit version(String version) {
    return new JUnit(version);
  }

  @Override
  public String caption() {
    return "JUnit " + version;
  }

  @Override
  public String locate(String module) {
    var jupiterVersion = version;
    var platformVersion = "1" + version.substring(1); // "5..." -> "1..."
    var guardianVersion = "1.1.2";
    var opentestVersion = "1.2.0";

    return switch (module) {
      case "org.apiguardian.api" -> central("org.apiguardian", "apiguardian-api", guardianVersion);
      case "org.junit.jupiter" -> central("org.junit.jupiter", "junit-jupiter", jupiterVersion);
      case "org.opentest4j" -> central("org.opentest4j", "opentest4j", opentestVersion);
      default -> {
        if (module.startsWith("org.junit.jupiter")) {
          var artifact = module.substring(4).replace('.', '-');
          yield central("org.junit.jupiter", artifact, jupiterVersion);
        }
        if (module.startsWith("org.junit.platform")) {
          var artifact = module.substring(4).replace('.', '-');
          yield central("org.junit.platform", artifact, platformVersion);
        }
        yield null;
      }
    };
  }
}
