package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Maven;
import java.util.Locale;
import java.util.Optional;

/** Implementations of {@link ModuleLookup} for various libraries. */
public class ModuleLookups {

  public static ModuleLookup ofJUnit_5_7() {
    return ModuleLookup.compose(
        ModuleLookup.linkExternalModule("org.apiguardian.api")
            .toMavenCentral("org.apiguardian", "apiguardian-api", "1.1.1"),
        ModuleLookup.linkExternalModule("org.opentest4j")
            .toMavenCentral("org.opentest4j", "opentest4j", "1.2.0"),
        new ModuleLookups.JUnitJupiter("5.7.0"),
        new ModuleLookups.JUnitPlatform("1.7.0"));
  }

  /** Maps well-known JavaFX module names to their Maven Central artifacts. */
  public static class JavaFX implements ModuleLookup {

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
     * Constructs a new module searcher with the given version.
     *
     * @param version the version
     */
    public JavaFX(String version) {
      this(version, classifier());
    }

    /**
     * Constructs a new module searcher with the given version.
     *
     * @param version the version
     * @param classifier the classifier
     */
    public JavaFX(String version, String classifier) {
      this.version = version;
      this.classifier = classifier;
    }

    @Override
    public Optional<String> lookupModule(String module) {
      if (!module.startsWith("javafx.")) return Optional.empty();
      var artifact = "javafx-" + module.substring(7).replace('.', '-');
      return Optional.of(Maven.central("org.openjfx", artifact, version, classifier));
    }
  }

  /** Maps well-known JUnit Jupiter module names to their Maven Central artifacts. */
  public static class JUnitJupiter implements ModuleLookup {

    private final String version;

    /**
     * Constructs a new module searcher with the given version.
     *
     * @param version the version
     */
    public JUnitJupiter(String version) {
      this.version = version;
    }

    private Optional<String> map(String suffix) {
      var artifact = "junit-jupiter" + (suffix.isEmpty() ? "" : '-' + suffix);
      return Optional.of(Maven.central("org.junit.jupiter", artifact, version));
    }

    @Override
    public Optional<String> lookupModule(String module) {
      return switch (module) {
        case "org.junit.jupiter" -> map("");
        case "org.junit.jupiter.api" -> map("api");
        case "org.junit.jupiter.engine" -> map("engine");
        case "org.junit.jupiter.migrationsupport" -> map("migrationsupport");
        case "org.junit.jupiter.params" -> map("params");
        default -> Optional.empty();
      };
    }
  }

  /** Maps well-known JUnit Platform module names to their Maven Central artifacts. */
  public static class JUnitPlatform implements ModuleLookup {

    private final String version;

    /**
     * Constructs a new module searcher with the given version.
     *
     * @param version the version
     */
    public JUnitPlatform(String version) {
      this.version = version;
    }

    private Optional<String> map(String suffix) {
      var artifact = "junit-platform-" + suffix.replace('.', '-');
      return Optional.of(Maven.central("org.junit.platform", artifact, version));
    }

    @Override
    public Optional<String> lookupModule(String module) {
      return switch (module) {
        case "org.junit.platform.commons" -> map("commons");
        case "org.junit.platform.console" -> map("console");
        case "org.junit.platform.engine" -> map("engine");
        case "org.junit.platform.jfr" -> map("jfr");
        case "org.junit.platform.launcher" -> map("launcher");
        case "org.junit.platform.reporting" -> map("reporting");
        case "org.junit.platform.suite.api" -> map("suite.api");
        case "org.junit.platform.testkit" -> map("testkit");
        default -> Optional.empty();
      };
    }
  }

  /** Maps well-known LWJGL module names to their Maven Central artifacts. */
  public static class LWJGL implements ModuleLookup {

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
     * Constructs a new module searcher with the given version.
     *
     * @param version the version
     */
    public LWJGL(String version) {
      this(version, classifier());
    }

    /**
     * Constructs a new module searcher with the given version.
     *
     * @param version the version
     * @param classifier the classifier
     */
    public LWJGL(String version, String classifier) {
      this.version = version;
      this.classifier = classifier;
    }

    @Override
    public Optional<String> lookupModule(String module) {
      if (!module.startsWith("org.lwjgl")) return Optional.empty();
      var natives = module.endsWith(".natives");
      var end = natives ? module.length() - 8 : module.length();
      var artifact = "lwjgl" + module.substring(9, end).replace('.', '-');
      return Optional.of(Maven.central("org.lwjgl", artifact, version, natives ? classifier : ""));
    }
  }

  /** Hidden default constructor. */
  private ModuleLookups() {}
}
