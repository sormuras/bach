package com.github.sormuras.bach.locator;

import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.ExternalModuleLocator;
import java.util.Locale;
import java.util.Optional;

/** Maps well-known JavaFX module names to their Maven Central artifacts. */
public record JavaFX(String version, String classifier) implements ExternalModuleLocator {

  public static final String MAVEN_GROUP = "org.openjfx";

  /**
   * Constructs a new JavaFX module locator with the given version.
   *
   * @param version the version
   */
  public static JavaFX of(String version) {
    return JavaFX.of(version, computeClassifier());
  }

  /**
   * Constructs a new JavaFX module locator ith the given version.
   *
   * @param version the version
   * @param classifier the classifier
   */
  public static JavaFX of(String version, String classifier) {
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
  public Stability stability() {
    return Stability.STABLE;
  }

  @Override
  public Optional<ExternalModuleLocation> locate(String module) {
    if (!module.startsWith("javafx.")) return Optional.empty();
    var artifact = "javafx-" + module.substring(7).replace('.', '-');
    var uri = Maven.central(MAVEN_GROUP, artifact, version, classifier);
    return Optional.of(new ExternalModuleLocation(module, uri));
  }

  @Override
  public String title() {
    return "javafx.[*] -> JavaFX " + version + '-' + classifier;
  }
}
