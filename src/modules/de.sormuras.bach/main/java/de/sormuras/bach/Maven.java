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

package de.sormuras.bach;

import java.net.URI;
import java.util.Properties;

/*BODY*/
/** Maven 2 repository support. */
public /*STATIC*/ class Maven {

  private final Log log;
  private final Resources resources;
  private final Properties moduleMavenProperties;
  private final Properties moduleVersionProperties;

  public Maven(
      Log log,
      Resources resources,
      Properties moduleMavenProperties,
      Properties moduleVersionProperties) {
    this.log = log;
    this.resources = resources;
    this.moduleMavenProperties = moduleMavenProperties;
    this.moduleVersionProperties = moduleVersionProperties;
  }

  public String lookup(String module) {
    return lookup(module, moduleVersionProperties.getProperty(module));
  }

  public String lookup(String module, String version) {
    return moduleMavenProperties.getProperty(module) + ':' + version;
  }

  public URI toUri(String group, String artifact, String version) {
    var repository =
        version.endsWith("SNAPSHOT")
            ? "https://oss.sonatype.org/content/repositories/snapshots"
            : "https://repo1.maven.org/maven2";
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
        var meta = resources.read(xml);
        var timestamp = substring(meta, "<timestamp>", "<");
        var buildNumber = substring(meta, "<buildNumber>", "<");
        var replacement = timestamp + '-' + buildNumber;
        log.debug("%s:%s:%s -> %s", group, artifact, version, replacement);
        file = file.replace("SNAPSHOT", replacement);
      } catch (Exception e) {
        log.warn("Maven metadata extraction from %s failed: %s", xml, e);
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
