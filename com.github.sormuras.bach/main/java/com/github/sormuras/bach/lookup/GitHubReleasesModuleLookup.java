package com.github.sormuras.bach.lookup;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.internal.GitHub;
import java.util.List;
import java.util.Optional;

/** Find modular JAR files attached to a GitHub Release. */
public class GitHubReleasesModuleLookup implements ModuleLookup {

  private final Bach bach;

  public GitHubReleasesModuleLookup(Bach bach) {
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

  @Override
  public String toString() {
    return "com.github.USER.REPOSITORY[.*] -> GitHub Releases";
  }
}
