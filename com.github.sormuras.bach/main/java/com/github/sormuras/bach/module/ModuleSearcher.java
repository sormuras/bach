package com.github.sormuras.bach.module;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.internal.GitHubReleasesSearcher;
import com.github.sormuras.bach.internal.MavenCentralSearcher;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Try to find a URI for a specific module name. */
@FunctionalInterface
public interface ModuleSearcher {
  /**
   * Returns an optional URI of the given module name.
   *
   * @param module the name of the module to find
   * @return a URI as a {@code String} wrapped in an optional object
   */
  Optional<String> search(String module);

  /**
   * Returns a module searcher that is composed from a sequence of zero or more module searcher.
   *
   * @param searchers the array of module searchers
   * @return a module searcher that composes a sequence of module searchers
   */
  static ModuleSearcher compose(ModuleSearcher... searchers) {
    var searcherList = List.of(searchers); // defensive copy and require non-null entries
    return module -> {
      for (var searcher : searcherList) {
        var result = searcher.search(module);
        if (result.isPresent()) return result;
      }
      return Optional.empty();
    };
  }

  /**
   * Returns a best-effort module searcher.
   *
   * @param bach the Java Shell Builder instance
   * @return a module searcher that tries to find a module in various locations
   */
  static ModuleSearcher ofBestEffort(Bach bach) {
    return compose(new GitHubReleasesSearcher(bach), new MavenCentralSearcher(bach));
  }

  /** Maps well-known JavaFX module names to their Maven Central artifacts. */
  class JavaFXSearcher implements ModuleSearcher {

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
    public JavaFXSearcher(String version) {
      this(version, classifier());
    }

    /**
     * Constructs a new module searcher with the given version.
     *
     * @param version the version
     * @param classifier the classifier
     */
    public JavaFXSearcher(String version, String classifier) {
      this.version = version;
      this.classifier = classifier;
    }

    @Override
    public Optional<String> search(String module) {
      if (!module.startsWith("javafx.")) return Optional.empty();
      var group = "org.openjfx";
      var artifact = "javafx-" + module.substring(7).replace('.', '-');
      var coordinates = group + ':' + artifact + ':' + version + ':' + classifier;
      return Optional.of(ModuleLink.link(module).toMavenCentral(coordinates).uri());
    }
  }

  /** Maps well-known JUnit Jupiter module names to their Maven Central artifacts. */
  class JUnitJupiterSearcher implements ModuleSearcher {

    private final String version;

    /**
     * Constructs a new module searcher with the given version.
     *
     * @param version the version
     */
    public JUnitJupiterSearcher(String version) {
      this.version = version;
    }

    Optional<String> map(String suffix) {
      var module = "org.junit.jupiter" + (suffix.isEmpty() ? "" : '.' + suffix);
      var artifact = "junit-jupiter" + (suffix.isEmpty() ? "" : '-' + suffix);
      var coordinates = "org.junit.jupiter" + ':' + artifact + ':' + version;
      var uri = ModuleLink.link(module).toMavenCentral(coordinates).uri();
      return Optional.of(uri);
    }

    @Override
    public Optional<String> search(String module) {
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
  class JUnitPlatformSearcher implements ModuleSearcher {

    private final String version;

    /**
     * Constructs a new module searcher with the given version.
     *
     * @param version the version
     */
    public JUnitPlatformSearcher(String version) {
      this.version = version;
    }

    Optional<String> map(String suffix) {
      var module = "org.junit.platform." + suffix;
      var artifact = "junit-platform-" + suffix;
      var coordinates = "org.junit.platform:" + artifact + ':' + version;
      var uri = ModuleLink.link(module).toMavenCentral(coordinates).uri();
      return Optional.of(uri);
    }

    @Override
    public Optional<String> search(String module) {
      return switch (module) {
        case "org.junit.platform.commons" -> map("commons");
        case "org.junit.platform.console" -> map("console");
        case "org.junit.platform.engine" -> map("engine");
        case "org.junit.platform.launcher" -> map("launcher");
        case "org.junit.platform.reporting" -> map("reporting");
        default -> Optional.empty();
      };
    }
  }

  /** Maps well-known LWJGL module names to their Maven Central artifacts. */
  class LWJGLSearcher implements ModuleSearcher {

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
    public LWJGLSearcher(String version) {
      this(version, classifier());
    }

    /**
     * Constructs a new module searcher with the given version.
     *
     * @param version the version
     * @param classifier the classifier
     */
    public LWJGLSearcher(String version, String classifier) {
      this.version = version;
      this.classifier = classifier;
    }

    @Override
    public Optional<String> search(String module) {
      if (!module.startsWith("org.lwjgl")) return Optional.empty();
      var group = "org.lwjgl";
      var natives = module.endsWith(".natives");
      var end = natives ? module.length() - 8 : module.length();
      var artifact = "lwjgl" + module.substring(9, end).replace('.', '-');
      var coordinates = group + ':' + artifact + ':' + version + (natives ? ":" + classifier : "");
      return Optional.of(ModuleLink.link(module).toMavenCentral(coordinates).uri());
    }
  }
}
