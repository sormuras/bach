package com.github.sormuras.bach.external;

/** Locates well-known JUnit Platform modules via their Maven Central artifacts. */
public class JUnitPlatformModuleLocator extends MavenCentralModuleLocator {

  public JUnitPlatformModuleLocator(String version) {
    super("org.junit.platform", version);
  }

  @Override
  public String title() {
    return "org.junit.platform[*] -> JUnit Platform " + version();
  }

  @Override
  protected ExternalModuleLocations newExternalModuleLocations() {
    return ExternalModuleLocations.of(
        location("org.junit.platform.commons", "junit-platform-commons"),
        location("org.junit.platform.console", "junit-platform-console"),
        location("org.junit.platform.engine", "junit-platform-engine"),
        location("org.junit.platform.jfr", "junit-platform-jfr"),
        location("org.junit.platform.launcher", "junit-platform-launcher"),
        location("org.junit.platform.reporting", "junit-platform-reporting"),
        location("org.junit.platform.suite.api", "junit-platform-suite-api"),
        location("org.junit.platform.suite.commons", "junit-platform-suite-commons"),
        location("org.junit.platform.suite.engine", "junit-platform-suite-engine"),
        location("org.junit.platform.testkit", "junit-platform-testkit"));
  }
}
