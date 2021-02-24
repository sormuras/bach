package com.github.sormuras.bach.lookup;

import java.util.Optional;

/**
 * A function that returns an optional string representation of a URI for a given module name.
 *
 * <p>See {@link ExternalModuleLookup} for an implementation that maps a single module name to a URI
 * in a stable manner. See {@link JUnitModuleLookup} for an implementation that maps a set of module
 * names to URIs that are related to a specific JUnit version.
 *
 * @see ExternalModuleLookup
 * @see JUnitModuleLookup
 */
@FunctionalInterface
public interface ModuleLookup {

  static ExternalModuleLookupBuilder external(String module) {
    return new ExternalModuleLookupBuilder(module);
  }

  static ModuleLookup ofJavaFX(String version) {
    return new JavaFXModuleLookup(version);
  }

  static ModuleLookup ofJUnit(String version) {
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
    return ofJUnit(version, "1" + version.substring(1), "1.1.1", "1.2.0");
  }

  static ModuleLookup ofJUnit(String jupiter, String platform, String guardian, String opentest) {
    return JUnitModuleLookup.of(jupiter, platform, guardian, opentest);
  }

  static ModuleLookup ofLWJGL(String version) {
    return new LWJGLModuleLookup(version);
  }

  /**
   * {@return a classification of the stability for this module lookup implementation}
   *
   * <p>Defaults to {@link LookupStability#UNKNOWN}.
   */
  default LookupStability lookupStability() {
    return LookupStability.UNKNOWN;
  }

  /**
   * {@return an optional string-representation of the given module name}
   *
   * @param module the name of the module to lookup
   */
  Optional<String> lookupUri(String module);
}
