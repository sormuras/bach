package com.github.sormuras.bach.external;

import com.github.sormuras.bach.ExternalModuleLocator;

/** Locates "Kotlin" modules via their Maven Central artifacts. */
public record Kotlin(String version) implements ExternalModuleLocator {

  private static final String MODULE_PREFIX = "kotlin";
  private static final String MAVEN_GROUP = "org.jetbrains.kotlin";

  /**
   * Constructs a new Kotlin module locator with the given version.
   *
   * @param version the Kotlin version
   */
  public static Kotlin version(String version) {
    return new Kotlin(version);
  }

  @Override
  public String caption() {
    return "kotlin.[*] -> Kotlin " + version;
  }

  @Override
  public String locate(String module) {
    if (!module.startsWith(MODULE_PREFIX)) return null;
    var artifact = module.replace('.', '-');
    return Maven.central(MAVEN_GROUP, artifact, version);
  }
}
