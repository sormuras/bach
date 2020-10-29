package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Bach;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;

/** GitHub-related helper. */
public class GitHub {

  private final Bach bach;
  private final String base;
  private final Pattern latestCommitHashPattern;
  private final Pattern latestReleaseTagPattern;

  public GitHub(Bach bach, String user, String repo) {
    this.bach = bach;
    this.base = "https://github.com/" + user + "/" + repo;
    this.latestCommitHashPattern = Pattern.compile("\"" + base + "/tree/(.{7}).*\"");
    this.latestReleaseTagPattern = Pattern.compile("\"/" + user + "/" + repo + "/tree/(.+?)\"");
  }

  public Optional<String> findLatestCommitHash() {
    return find(browse(base + "/tree/HEAD"), latestCommitHashPattern);
  }

  public Optional<String> findLatestReleaseTag() {
    var page = browse(base + "/releases/latest");
    return find(page, latestReleaseTagPattern);
  }

  public Optional<String> findReleasedModule(String module, String version) {
    var file = module + '@' + version + ".jar";
    var path = String.join("/", "releases/download", version, file);
    var page = browse(base + "/releases/tag/" + version);
    return page.contains(file) ? Optional.of(base + '/' + path) : Optional.empty();
  }

  private String browse(String uri) {
    return bach.httpRead(URI.create(uri));
  }

  private static Optional<String> find(String string, Pattern pattern) {
    var matcher = pattern.matcher(string);
    if (matcher.find()) {
      var encoded = matcher.group(1);
      var decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
      return Optional.of(decoded);
    }
    return Optional.empty();
  }
}
