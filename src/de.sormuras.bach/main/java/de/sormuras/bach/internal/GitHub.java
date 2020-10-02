/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach.internal;

import java.net.URI;
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
    if (!page.contains(path)) return Optional.empty();
    return Optional.of(base + '/' + path);
  }

  private static String browse(String url) {
    try (var stream = URI.create(url).toURL().openStream()) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      return e.toString();
    }
  }

  private static Optional<String> find(String string, Pattern pattern) {
    var matcher = pattern.matcher(string);
    if (matcher.find()) return Optional.of(matcher.group(1));
    return Optional.empty();
  }
}
