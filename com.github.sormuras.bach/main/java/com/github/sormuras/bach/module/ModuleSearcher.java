package com.github.sormuras.bach.module;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.internal.GitHubReleasesSearcher;
import com.github.sormuras.bach.internal.MavenCentralSearcher;
import java.util.List;
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

  /** Maps well-known JUnit Jupiter module names to their Maven Central artifacts. */
  class JUnitJupiterSearcher implements ModuleSearcher {

    private final String version;

    /**
     * Constructs a new module searcher with the given version.
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
}
