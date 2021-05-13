package com.github.sormuras.bach.locator;

import com.github.sormuras.bach.api.ExternalModuleLocations;

/** Locates well-known JUnit Jupiter modules via their Maven Central artifacts. */
public class JUnitJupiterModuleLocator extends MavenCentralModuleLocator {

  public JUnitJupiterModuleLocator(String version) {
    super("org.junit.jupiter", version);
  }

  @Override
  public String title() {
    return "org.junit.jupiter[*] -> JUnit Jupiter " + version();
  }

  @Override
  protected ExternalModuleLocations newExternalModuleLocations() {
    return ExternalModuleLocations.of(
        location("org.junit.jupiter", "junit-jupiter"),
        location("org.junit.jupiter.api", "junit-jupiter-api"),
        location("org.junit.jupiter.engine", "junit-jupiter-engine"),
        location("org.junit.jupiter.migrationsupport", "junit-jupiter-migrationsupport"),
        location("org.junit.jupiter.params", "junit-jupiter-params"));
  }
}
