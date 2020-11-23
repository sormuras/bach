package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.module.ModuleSearcher;
import java.util.List;
import java.util.Optional;

/**
 * Search for a modular JAR file attached to a GitHub release.
 */
public class GitHubReleasesSearcher implements ModuleSearcher {

  private final Bach bach;

  public GitHubReleasesSearcher(Bach bach) {
    this.bach = bach;
  }

  @Override
  public Optional<String> search(String module) {
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
