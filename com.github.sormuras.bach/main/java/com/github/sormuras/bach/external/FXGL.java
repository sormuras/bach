package com.github.sormuras.bach.external;

import com.github.sormuras.bach.ModuleLocator;

/** Locates "Java/JavaFX/Kotlin Game Library" modules via their Maven Central artifacts. */
public record FXGL(String version) implements ModuleLocator {

  private static final String MODULE_PREFIX = "com.almasb.fxgl";
  private static final String MAVEN_GROUP = "com.github.almasb";

  /**
   * Constructs a new FXGL module locator with the given version.
   *
   * @param version the FXGL version
   */
  public static FXGL version(String version) {
    return new FXGL(version);
  }

  @Override
  public String caption() {
    return "com.almasb.fxgl[*] -> FXGL " + version;
  }

  @Override
  public String locate(String module) {
    if (!module.startsWith(MODULE_PREFIX)) return null;
    var artifact = module.equals("com.almasb.fxgl.all") ? "fxgl" : "fxgl-" + module.substring(16);
    return Maven.central(MAVEN_GROUP, artifact, version);
  }
}
