package com.github.sormuras.bach.lookup;

import java.util.Optional;

/** Lookup well-known JUnit modules published at Maven Central. */
public enum JUnit implements ModuleLookup {

  /** Lookup modules of JUnit 5.8.0-M1 via their Maven Central artifacts. */
  V_5_8_0_M1("5.8.0-M1", "1.8.0-M1", "1.1.1", "1.2.0"),

  /** Lookup modules of JUnit 5.7.1 via their Maven Central artifacts. */
  V_5_7_1("5.7.1", "1.7.1", "1.1.1", "1.2.0"),

  /** Lookup modules of JUnit 5.7.0 via their Maven Central artifacts. */
  V_5_7_0("5.7.0", "1.7.0", "1.1.1", "1.2.0");

  private final ModuleLookup junit;

  JUnit(String jupiter, String platform, String apiguardian, String opentest4j) {
    this.junit = JUnitModuleLookup.of(jupiter, platform, apiguardian, opentest4j);
  }

  @Override
  public Optional<String> lookupUri(String module) {
    return junit.lookupUri(module);
  }

  @Override
  public String toString() {
    return junit.toString();
  }
}
