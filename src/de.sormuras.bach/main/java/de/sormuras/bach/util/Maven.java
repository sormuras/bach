/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

package de.sormuras.bach.util;

import de.sormuras.bach.Log;
import javax.lang.model.SourceVersion;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Maven 2 repository support. */
public class Maven {

  public static class Lookup implements UnaryOperator<String> {

    final UnaryOperator<String> custom;
    final Map<String, String> library;
    final Set<Pattern> libraryPatterns;
    final Map<String, String> fallback;

    public Lookup(
        UnaryOperator<String> custom, Map<String, String> library, Map<String, String> fallback) {
      this.custom = custom;
      this.library = library;
      this.fallback = fallback;
      this.libraryPatterns =
          library.keySet().stream()
              .map(Object::toString)
              .filter(key -> !SourceVersion.isName(key))
              .map(Pattern::compile)
              .collect(Collectors.toSet());
    }

    @Override
    public String apply(String module) {
      try {
        var custom = this.custom.apply(module);
        if (custom != null) {
          return custom;
        }
      } catch (Modules.UnmappedModuleException e) {
        // fall-through
      }
      var library = this.library.get(module);
      if (library != null) {
        return library;
      }
      if (libraryPatterns.size() > 0) {
        for (var pattern : libraryPatterns) {
          if (pattern.matcher(module).matches()) {
            return this.library.get(pattern.pattern());
          }
        }
      }
      var fallback = this.fallback.get(module);
      if (fallback != null) {
        return fallback;
      }
      throw new Modules.UnmappedModuleException(module);
    }
  }

  private final Log log;
  private final Uris uris;
  private final Lookup groupArtifacts;
  private final Lookup versions;

  public Maven(Log log, Uris uris, Lookup groupArtifacts, Lookup versions) {
    this.log = log;
    this.uris = uris;
    this.groupArtifacts = groupArtifacts;
    this.versions = versions;
  }

  public String lookup(String module) {
    return lookup(module, versions.apply(module));
  }

  public String lookup(String module, String version) {
    return groupArtifacts.apply(module) + ':' + version;
  }

  public URI toUri(String repository, String group, String artifact, String version) {
    return toUri(repository, group, artifact, version, "", "jar");
  }

  public URI toUri(
      String repository,
      String group,
      String artifact,
      String version,
      String classifier,
      String type) {
    var versionAndClassifier = classifier.isBlank() ? version : version + '-' + classifier;
    var file = artifact + '-' + versionAndClassifier + '.' + type;
    if (version.endsWith("SNAPSHOT")) {
      var base = String.join("/", repository, group.replace('.', '/'), artifact, version);
      var xml = URI.create(base + "/maven-metadata.xml");
      try {
        var meta = uris.read(xml);
        var timestamp = substring(meta, "<timestamp>", "<");
        var buildNumber = substring(meta, "<buildNumber>", "<");
        var replacement = timestamp + '-' + buildNumber;
        log.debug("%s:%s:%s -> %s", group, artifact, version, replacement);
        file = file.replace("SNAPSHOT", replacement);
      } catch (Exception e) {
        log.warning("Maven metadata extraction from %s failed: %s", xml, e);
      }
    }
    var uri = String.join("/", repository, group.replace('.', '/'), artifact, version, file);
    return URI.create(uri);
  }

  /** Extract substring between begin and end tags. */
  static String substring(String string, String beginTag, String endTag) {
    int beginIndex = string.indexOf(beginTag) + beginTag.length();
    int endIndex = string.indexOf(endTag, beginIndex);
    return string.substring(beginIndex, endIndex).trim();
  }
}
