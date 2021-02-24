package com.github.sormuras.bach.lookup;

import java.util.Locale;
import java.util.Optional;

/** Maps well-known JavaFX module names to their Maven Central artifacts. */
public class JavaFXModuleLookup implements ModuleLookup {

  /** @return the classifier determined via the {@code os.name} system property */
  public static String classifier() {
    var os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    var isWindows = os.contains("win");
    var isMac = os.contains("mac");
    var isLinux = !(isWindows || isMac);
    return isLinux ? "linux" : isMac ? "mac" : "win";
  }

  private final String version;
  private final String classifier;

  /**
   * Constructs a new JavaFX module lookup with the given version.
   *
   * @param version the version
   */
  public JavaFXModuleLookup(String version) {
    this(version, classifier());
  }

  /**
   * Constructs a new JavaFX module lookup with the given version.
   *
   * @param version the version
   * @param classifier the classifier
   */
  public JavaFXModuleLookup(String version, String classifier) {
    this.version = version;
    this.classifier = classifier;
  }

  @Override
  public LookupStability lookupStability() {
    return LookupStability.STABLE;
  }

  @Override
  public Optional<String> lookupUri(String module) {
    if (!module.startsWith("javafx.")) return Optional.empty();
    var artifact = "javafx-" + module.substring(7).replace('.', '-');
    return Optional.of(Maven.central("org.openjfx", artifact, version, classifier));
  }

  @Override
  public String toString() {
    return "javafx.[*] -> JavaFX " + version + '-' + classifier;
  }
}
