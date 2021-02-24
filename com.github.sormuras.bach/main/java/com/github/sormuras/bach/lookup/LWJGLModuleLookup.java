package com.github.sormuras.bach.lookup;

import java.util.Locale;
import java.util.Optional;

/** Maps well-known LWJGL module names to their Maven Central artifacts. */
public class LWJGLModuleLookup implements ModuleLookup {

  /** @return the classifier determined via the {@code os.name} system property */
  public static String classifier() {
    var os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    var isWindows = os.contains("win");
    var isMac = os.contains("mac");
    var isLinux = !(isWindows || isMac);
    var arch = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);
    var is64 = arch.contains("64");
    if (isLinux) {
      if (arch.startsWith("arm") || arch.startsWith("aarch64")) {
        var arm64 = is64 || arch.startsWith("armv8");
        return arm64 ? "natives-linux-arm64" : "natives-linux-arm32";
      }
      return "natives-linux";
    }
    if (isWindows) {
      return is64 ? "natives-windows" : "natives-windows-x86";
    }
    return "natives-macos";
  }

  private final String version;
  private final String classifier;

  /**
   * Constructs a new LWJGL module lookup with the given version.
   *
   * @param version the version
   */
  public LWJGLModuleLookup(String version) {
    this(version, classifier());
  }

  /**
   * Constructs a new LWJGL module lookup with the given version.
   *
   * @param version the version
   * @param classifier the classifier
   */
  public LWJGLModuleLookup(String version, String classifier) {
    this.version = version;
    this.classifier = classifier;
  }

  @Override
  public LookupStability lookupStability() {
    return LookupStability.STABLE;
  }

  @Override
  public Optional<String> lookupUri(String module) {
    if (!module.startsWith("org.lwjgl")) return Optional.empty();
    var natives = module.endsWith(".natives");
    var end = natives ? module.length() - 8 : module.length();
    var artifact = "lwjgl" + module.substring(9, end).replace('.', '-');
    return Optional.of(Maven.central("org.lwjgl", artifact, version, natives ? classifier : ""));
  }

  @Override
  public String toString() {
    return "org.lwjgl[*] -> LWJGL " + version + '-' + classifier;
  }
}
