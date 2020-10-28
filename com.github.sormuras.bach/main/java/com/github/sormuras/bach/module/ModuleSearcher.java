package com.github.sormuras.bach.module;

import com.github.sormuras.bach.internal.GitHub;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/** Try to find an optional URI for a specific module name. */
@FunctionalInterface
public interface ModuleSearcher {
  /**
   * Returns an optional URI of the given module name.
   *
   * @param module the name of the module to find
   * @return a URI wrapped in an optional object
   */
  Optional<URI> search(String module);

  /**
   * Returns a module searcher that is composed from a sequence of zero or more module searcher.
   *
   * @param searchers the array of module searchers
   * @return a module searcher that composes a sequence of module searchers
   */
  static ModuleSearcher compose(ModuleSearcher... searchers) {
    var searcherList = List.of(searchers);
    return module -> {
      for (var searcher : searcherList) {
        var result = searcher.search(module);
        if (result.isPresent()) return result;
      }
      return Optional.empty();
    };
  }

  static ModuleSearcher dynamic() {
    return compose(
        ModuleSearcher::searchGitHubReleases
        // searchGitHubPackages
        // searchMavenCentralViaSormurasModules
    );
  }

  /**
   * Search for a module JAR file attached to a GitHub release.
   *
   * @param module the name of the module to find
   * @return a URI wrapped in an optional object
   */
  static Optional<URI> searchGitHubReleases(String module) {
    if (!module.startsWith("com.github.")) return Optional.empty();
    var split = module.split("\\.");
    if (split.length < 4) return Optional.empty();
    assert "com".equals(split[0]);
    assert "github".equals(split[1]);
    var github = new GitHub(split[2], split[3]);
    var latest = github.findLatestReleaseTag();
    if (latest.isPresent()) {
      var version = latest.orElseThrow();
      return github.findReleasedModule(module, version).map(URI::create);
    }
    for (var tag : List.of("early-access", "ea", "latest", "snapshot")) {
      var candidate = github.findReleasedModule(module, tag);
      if (candidate.isPresent()) return candidate.map(URI::create);
    }
    return Optional.empty();
  }
}
