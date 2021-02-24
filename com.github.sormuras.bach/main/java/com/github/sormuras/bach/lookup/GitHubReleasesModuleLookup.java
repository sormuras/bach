package com.github.sormuras.bach.lookup;

import com.github.sormuras.bach.Bach;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Find modular JAR files attached to a GitHub Release. */
public class GitHubReleasesModuleLookup implements ModuleLookup {

  private final Bach bach;

  public GitHubReleasesModuleLookup(Bach bach) {
    this.bach = bach;
  }

  @Override
  public LookupStability lookupStability() {
    return LookupStability.DYNAMIC;
  }

  @Override
  public Optional<String> lookupUri(String module) {
    if (!module.startsWith("com.github.")) return Optional.empty();
    var split = module.split("\\.");
    if (split.length < 4) return Optional.empty();
    assert "com".equals(split[0]);
    assert "github".equals(split[1]);
    var github = new GitHub(split[2], split[3]);
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

  class GitHub {

    private final String base;
    private final Pattern latestCommitHashPattern;
    private final Pattern latestReleaseTagPattern;

    GitHub(String user, String repo) {
      this.base = "https://github.com/" + user + "/" + repo;
      this.latestCommitHashPattern = Pattern.compile("\"" + base + "/tree/(.{7}).*\"");
      this.latestReleaseTagPattern = Pattern.compile("\"/" + user + "/" + repo + "/tree/(.+?)\"");
    }

    Optional<String> findLatestCommitHash() {
      return find(browse(base + "/tree/HEAD"), latestCommitHashPattern);
    }

    Optional<String> findLatestReleaseTag() {
      var page = browse(base + "/releases/latest");
      return find(page, latestReleaseTagPattern);
    }

    Optional<String> findReleasedModule(String module, String version) {
      var file = module + '@' + version + ".jar";
      var path = String.join("/", "releases/download", version, file);
      var page = browse(base + "/releases/tag/" + version);
      return page.contains(file) ? Optional.of(base + '/' + path) : Optional.empty();
    }

    String browse(String uri) {
      return bach.browser().read(uri);
    }

    static Optional<String> find(String string, Pattern pattern) {
      var matcher = pattern.matcher(string);
      if (matcher.find()) {
        var encoded = matcher.group(1);
        var decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        return Optional.of(decoded);
      }
      return Optional.empty();
    }
  }
}
