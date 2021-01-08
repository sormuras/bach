package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.GitHub;
import com.github.sormuras.bach.internal.Maven;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Implementations of {@link ModuleLookup} for various libraries. */
public class ModuleLookups {

  /** Find modular JAR files attached to a GitHub Release. */
  public static class GitHubReleases implements ModuleLookup {

    private final Bach bach;

    public GitHubReleases(Bach bach) {
      this.bach = bach;
    }

    @Override
    public Optional<String> lookupModule(String module) {
      if (!module.startsWith("com.github.")) return Optional.empty();
      var split = module.split("\\.");
      if (split.length < 4) return Optional.empty();
      assert "com".equals(split[0]);
      assert "github".equals(split[1]);
      var github = new GitHub(bach, split[2], split[3]);
      var latest = github.findLatestReleaseTag();
      if (latest.isPresent()) {
        var releasedModule = github.findReleasedModule(module, latest.get());
        if (releasedModule.isPresent()) return releasedModule;
      }
      for (var tag : List.of("early-access", "ea", "latest", "snapshot")) {
        var candidate = github.findReleasedModule(module, tag);
        if (candidate.isPresent()) return candidate;
      }
      return Optional.empty();
    }
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

  /** Link well-known JUnit module to their Maven Central artifacts. */
  public enum JUnit implements ModuleLookup {

    /** Link modules of JUnit 5.7.0 to their Maven Central artifacts. */
    V_5_7_0("5.7.0", "1.7.0", "1.1.1", "1.2.0");

    private final ModuleLookup junit;

    JUnit(String jupiter, String platform, String apiguardian, String opentest4j) {
      this.junit =
          ModuleLookup.compose(
              new JUnitJupiter(jupiter),
              new JUnitPlatform(platform),
              ModuleLookup.linkExternalModule("org.apiguardian.api")
                  .toMavenCentral("org.apiguardian", "apiguardian-api", apiguardian),
              ModuleLookup.linkExternalModule("org.opentest4j")
                  .toMavenCentral("org.opentest4j", "opentest4j", opentest4j));
    }

    @Override
    public Optional<String> lookupModule(String module) {
      return junit.lookupModule(module);
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
