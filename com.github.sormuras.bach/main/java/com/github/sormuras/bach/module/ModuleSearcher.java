package com.github.sormuras.bach.module;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.internal.GitHubReleasesSearcher;
import com.github.sormuras.bach.internal.MavenCentralSearcher;
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
}
