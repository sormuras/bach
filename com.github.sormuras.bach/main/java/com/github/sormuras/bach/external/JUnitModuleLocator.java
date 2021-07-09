package com.github.sormuras.bach.external;

import java.util.List;
import java.util.Optional;

/** Locates well-known JUnit modules published at Maven Central. */
record JUnitModuleLocator(String version, List<ExternalModuleLocator> locators)
    implements ExternalModuleLocator {

  static JUnitModuleLocator of(
      String jupiterVersion,
      String platformVersion,
      String apiguardianVersion,
      String opentest4jVersion) {
    var locators =
        List.of(
            new JUnitJupiterModuleLocator(jupiterVersion),
            new JUnitPlatformModuleLocator(platformVersion),
            new ExternalModuleLocation(
                "org.junit.vintage.engine",
                Maven.central("org.junit.vintage", "junit-vintage-engine", jupiterVersion)),
            new ExternalModuleLocation(
                "org.apiguardian.api",
                Maven.central("org.apiguardian", "apiguardian-api", apiguardianVersion)),
            new ExternalModuleLocation(
                "org.opentest4j",
                Maven.central("org.opentest4j", "opentest4j", opentest4jVersion)));

    return new JUnitModuleLocator(jupiterVersion, locators);
  }

  @Override
  public Optional<ExternalModuleLocation> locate(String module) {
    for (var locator : locators) {
      var location = locator.locate(module);
      if (location.isPresent()) return location;
    }
    return Optional.empty();
  }

  @Override
  public String title() {
    return "JUnit " + version();
  }
}
