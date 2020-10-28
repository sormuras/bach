package com.github.sormuras.bach.internal;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;

/** GitHub-related helper. */
public class GitHub {

  private final String base;
  private final Pattern latestCommitHashPattern;
  private final Pattern latestReleaseTagPattern;

  public GitHub(String user, String repo) {
    this.base = "https://github.com/" + user + "/" + repo;
    this.latestCommitHashPattern = Pattern.compile("\"" + base + "/tree/(.{7}).*\"");
    this.latestReleaseTagPattern = Pattern.compile("\"/" + user + "/" + repo + "/tree/(.+?)\"");
  }

  public Optional<String> findLatestCommitHash() {
    return find(browse(base + "/tree/HEAD"), latestCommitHashPattern);
  }

  public Optional<String> findLatestReleaseTag() {
    return find(browse(base + "/releases/latest"), latestReleaseTagPattern);
  }

  public Optional<String> findReleasedModule(String module, String version) {
    var file = module + '@' + version + ".jar";
    var path = String.join("/", "releases/download", version, file);
    var page = browse(base + "/releases/tag/" + version);
    return page.contains(file) ? Optional.of(base + '/' + path) : Optional.empty();
  }

  private static String browse(String uri) {
    try (var stream = URI.create(uri).toURL().openStream()) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      return e.toString();
    }
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
