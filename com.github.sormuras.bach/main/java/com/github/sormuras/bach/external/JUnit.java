package com.github.sormuras.bach.external;

import java.util.Optional;

/** Locate well-known JUnit modules published at Maven Central. */
public enum JUnit implements ExternalModuleLocator {

  /** Lookup modules of JUnit 5.8.0-M1 via their Maven Central artifacts. */
  V_5_8_0_M1("5.8.0-M1", "1.8.0-M1", "1.1.1", "1.2.0"),

  /** Lookup modules of JUnit 5.7.2 via their Maven Central artifacts. */
  V_5_7_2("5.7.2", "1.7.2", "1.1.1", "1.2.0");

  public static ExternalModuleLocator of(String version) {
    try {
      return JUnit.valueOf(version);
    } catch (IllegalArgumentException ignore) {
      // fall-through
    }
    try {
      var name = "V_" + version.replace('.', '_').replace('-', '_');
      return JUnit.valueOf(name);
    } catch (IllegalArgumentException ignore) {
      // fall-through
    }
    if (!version.startsWith("5.")) {
      throw new IllegalArgumentException("JUnit version must start with `5.`, but got: " + version);
    }
    return JUnit.of(version, "1" + version.substring(1), "1.1.1", "1.2.0");
  }

  public static ExternalModuleLocator of(
      String jupiterVersion,
      String platformVersion,
      String guardianVersion,
      String opentestVersion) {
    return JUnitModuleLocator.of(jupiterVersion, platformVersion, guardianVersion, opentestVersion);
  }

  private final ExternalModuleLocator junit;

  JUnit(String jupiter, String platform, String apiguardian, String opentest4j) {
    this.junit = JUnitModuleLocator.of(jupiter, platform, apiguardian, opentest4j);
  }

  @Override
  public Stability stability() {
    return Stability.STABLE;
  }

  @Override
  public Optional<ExternalModuleLocation> locate(String module) {
    return junit.locate(module);
  }

  @Override
  public String title() {
    return junit.title();
  }
}
