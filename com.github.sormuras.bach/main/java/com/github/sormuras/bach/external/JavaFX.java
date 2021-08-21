package com.github.sormuras.bach.external;

import com.github.sormuras.bach.ExternalModuleLocator;
import java.util.Locale;

/** Locates well-known JavaFX modules via their Maven Central artifacts. */
public record JavaFX(String version, String classifier) implements ExternalModuleLocator {

  public static final String MAVEN_GROUP = "org.openjfx";

  /**
   * Constructs a new JavaFX module locator with the given version.
   *
   * @param version the JavaFX version
   */
  public static JavaFX version(String version) {
    return JavaFX.version(version, computeClassifier());
  }

  /**
   * Constructs a new JavaFX module locator with the given version.
   *
   * @param version the JavaFX version
   * @param classifier the classifier normally identifying the platform
   */
  public static JavaFX version(String version, String classifier) {
    return new JavaFX(version, classifier);
  }

  /** @return the classifier determined via the {@code os.name} system property */
  private static String computeClassifier() {
    var os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    var isWindows = os.contains("win");
    var isMac = os.contains("mac");
    var isLinux = !(isWindows || isMac);
    return isLinux ? "linux" : isMac ? "mac" : "win";
  }

  @Override
  public String caption() {
    return "javafx.[*] -> JavaFX " + version + '-' + classifier;
  }

  @Override
  public String locate(String module) {
    if (!module.startsWith("javafx.")) return null;
    var artifact = "javafx-" + module.substring(7);
    return Maven.central(MAVEN_GROUP, artifact, version, classifier);
  }
}
