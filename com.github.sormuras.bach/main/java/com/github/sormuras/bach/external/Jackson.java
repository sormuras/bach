package com.github.sormuras.bach.external;

import com.github.sormuras.bach.ExternalModuleLocator;

/** Locates "Jackson" modules via their Maven Central artifacts. */
public record Jackson(String version) implements ExternalModuleLocator {

  private static final String MODULE_PREFIX = "com.fasterxml.jackson";
  private static final String MAVEN_GROUP = "com.fasterxml.jackson";

  /**
   * Constructs a new Jackson module locator with the given version.
   *
   * @param version the Jackson version
   */
  public static Jackson version(String version) {
    return new Jackson(version);
  }

  @Override
  public String caption() {
    return "com.fasterxml.jackson.[*] -> Jackson " + version;
  }

  @Override
  public String locate(String module) {
    if (!module.startsWith(MODULE_PREFIX)) return null;
    var group =
        switch (module) {
          case "com.fasterxml.jackson.annotation",
              "com.fasterxml.jackson.core",
              "com.fasterxml.jackson.databind" -> "com.fasterxml.jackson.core";
          default -> MAVEN_GROUP;
        };
    var artifact =
        switch (module) {
          case "com.fasterxml.jackson.annotation" -> "jackson-annotations";
          default -> module.substring(14).replace('.', '-');
        };
    return Maven.central(group, artifact, version);
  }
}
