package com.github.sormuras.bach.lookup;

import java.util.Optional;

@FunctionalInterface
public interface ModuleLookup {

  static ExternalModuleLookupBuilder external(String module) {
    return new ExternalModuleLookupBuilder(module);
  }

  static ModuleLookup ofJavaFX(String version) {
    return new JavaFXModuleLookup(version);
  }

  static ModuleLookup ofJUnit(String version) {
    if (!version.startsWith("5.")) throw new IllegalArgumentException("");
    return ofJUnit(version, "1" + version.substring(1), "1.1.1", "1.2.0");
  }

  static ModuleLookup ofJUnit(String jupiter, String platform, String guardian, String opentest) {
    return JUnitModuleLookup.of(jupiter, platform, guardian, opentest);
  }

  static ModuleLookup ofLWJGL(String version) {
    return new LWJGLModuleLookup(version);
  }

  Optional<String> lookupUri(String module);
}
