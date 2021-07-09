package com.github.sormuras.bach.external;

import java.util.Optional;

/** Maps "Java/JavaFX/Kotlin Game Library" module names to their Maven Central artifacts. */
public record FXGL(String version) implements ExternalModuleLocator {

  private static final String MODULE_PREFIX = "com.almasb.fxgl";
  private static final String MAVEN_GROUP = "com.github.almasb";

  /**
   * Constructs a new FXGL module locator with the given version.
   *
   * @param version the version
   */
  public static FXGL of(String version) {
    return new FXGL(version);
  }

  @Override
  public Stability stability() {
    return Stability.STABLE;
  }

  @Override
  public Optional<ExternalModuleLocation> locate(String module) {
    if (!module.startsWith(MODULE_PREFIX)) return Optional.empty();
    var artifact =
        module.equals("com.almasb.fxgl.all")
            ? "fxgl"
            : "fxgl-" + module.substring(MODULE_PREFIX.length() + 1);
    var uri = Maven.central(MAVEN_GROUP, artifact, version);
    return Optional.of(new ExternalModuleLocation(module, uri));
  }

  @Override
  public String title() {
    return "com.almasb.fxgl[*] -> FXGL " + version;
  }
}
