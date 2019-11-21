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
import de.sormuras.bach.project.Deployment;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.Unit;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
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

  public String version(String module) {
    return versions.apply(module);
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

  public static class Scribe {

    enum ScriptType {
      BASH(".sh", '\''),
      WIN(".bat", '"') {
        @Override
        List<String> lines(List<String> lines) {
          return lines.stream().map(line -> "call " + line).collect(Collectors.toList());
        }
      };

      final String extension;
      final char quote;

      ScriptType(String extension, char quote) {
        this.extension = extension;
        this.quote = quote;
      }

      String quote(Object object) {
        return quote + object.toString() + quote;
      }

      List<String> lines(List<String> lines) {
        return lines;
      }
    }

    final Project project;

    public Scribe(Project project) {
      this.project = project;
    }

    public void generateMavenInstallScript(Iterable<Unit> units) {
      for (var type : ScriptType.values()) {
        generateMavenInstallScript(type, units);
      }
    }

    void generateMavenInstallScript(ScriptType type, Iterable<Unit> units) {
      var plugin = "install:install-file";
      var maven = String.join(" ", "mvn", "--batch-mode", "--no-transfer-progress", plugin);
      var lines = new ArrayList<String>();
      for (var unit : units) {
        if (unit.mavenPom().isPresent()) {
          lines.add(String.join(" ", maven, generateMavenArtifactLine(unit, type)));
        }
      }
      if (lines.isEmpty()) {
        // log("No maven-install script lines generated.");
        return;
      }
      try {
        var script = project.folder().out("maven-install" + type.extension);
        Files.write(script, type.lines(lines));
      } catch (Exception e) {
        throw new RuntimeException("Generating install script failed: " + e.getMessage(), e);
      }
    }

    public void generateMavenDeployScript(Iterable<Unit> units) {
      var deployment = project.deployment();
      if (deployment.isEmpty()) {
        // log("No Maven deployment record available.");
        return;
      }
      for (var type : ScriptType.values()) {
        generateMavenDeployScript(type, deployment.get(), units);
      }
    }

    void generateMavenDeployScript(ScriptType type, Deployment deployment, Iterable<Unit> units) {
      var plugin = "org.apache.maven.plugins:maven-deploy-plugin:3.0.0-M1:deploy-file";
      var repository = "repositoryId=" + type.quote(deployment.mavenRepositoryId());
      var url = "url=" + type.quote(deployment.mavenUri());
      var maven = String.join(" ", "mvn", "--batch-mode", plugin);
      var repoAndUrl = String.join(" ", "-D" + repository, "-D" + url);
      var lines = new ArrayList<String>();
      for (var unit : units) {
        lines.add(String.join(" ", maven, repoAndUrl, generateMavenArtifactLine(unit, type)));
      }
      if (lines.isEmpty()) {
        // log("No maven-deploy script lines generated.");
        return;
      }
      try {
        var name = "maven-deploy-" + deployment.mavenRepositoryId();
        var script = project.folder().out(name + type.extension);
        Files.write(script, type.lines(lines));
      } catch (Exception e) {
        throw new RuntimeException("Deploy failed: " + e.getMessage(), e);
      }
    }

    String generateMavenArtifactLine(Unit unit, ScriptType type) {
      var pom = "pomFile=" + type.quote(unit.mavenPom().orElseThrow());
      var file = "file=" + type.quote(project.modularJar(unit));
      var sources = "sources=" + type.quote(project.sourcesJar(unit));
      var javadoc = "javadoc=" + type.quote(project.javadocJar(unit.realm()));
      return String.join(" ", "-D" + pom, "-D" + file, "-D" + sources, "-D" + javadoc);
    }
  }
}
