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

import de.sormuras.bach.Bach;
import de.sormuras.bach.project.Deployment;
import de.sormuras.bach.project.Library;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.Template;
import de.sormuras.bach.project.Template.Placeholder;
import de.sormuras.bach.project.Unit;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/** Maven 2 repository support. */
public class Maven {

  public static class Central {

    private final Map<String, String> mavens;
    private final Map<String, String> versions;

    public Central(Uris uris) throws Exception {
      var user = Path.of(System.getProperty("user.home"));
      var cache = Files.createDirectories(user.resolve(".bach/modules"));
      var artifactPath =
          uris.copy(
              URI.create("https://github.com/sormuras/modules/raw/master/module-maven.properties"),
              cache.resolve("module-maven.properties"),
              StandardCopyOption.COPY_ATTRIBUTES);
      this.mavens = map(Paths.load(new Properties(), artifactPath));
      var versionPath =
          uris.copy(
              URI.create(
                  "https://github.com/sormuras/modules/raw/master/module-version.properties"),
              cache.resolve("module-version.properties"),
              StandardCopyOption.COPY_ATTRIBUTES);
      this.versions = map(Paths.load(new Properties(), versionPath));
    }

    public Library.Link link(String module) {
      var maven = mavens.get(module);
      if (maven == null) throw new Modules.UnmappedModuleException(module);
      var indexOfColon = maven.indexOf(':');
      if (indexOfColon < 0) throw new AssertionError("Expected group:artifact, but got: " + maven);
      var group = maven.substring(0, indexOfColon);
      var artifact = maven.substring(indexOfColon + 1);
      var version = versions.get(module);
      if (version == null) throw new Modules.UnmappedModuleException(module);
      return Library.Link.central(group, artifact, version);
    }
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

    final Bach bach;
    final Project project;

    public Scribe(Bach bach) {
      this.bach = bach;
      this.project = bach.getProject();
    }

    public void generateMavenPoms(Iterable<Unit> units) throws Exception {
      var projectPom = project.deployment().mavenPomTemplate();
      for (var unit : units) {
        var path = unit.mavenPom().orElse(projectPom);
        if (!Files.isRegularFile(path)) continue;
        var template = Paths.readString(path);
        var group = group(template);
        var binding =
            Map.of(
                Placeholder.GROUP, group,
                Placeholder.MODULE, unit.name(),
                Placeholder.VERSION, project.version(unit).toString());
        var lines = new ArrayList<String>();
        for (var line : template.lines().collect(Collectors.toList())) {
          var DEPENDENCIES = "<!--${DEPENDENCIES}-->";
          if (DEPENDENCIES.equals(line.strip())) {
            var indent = line.substring(0, line.indexOf(DEPENDENCIES));
            for (var requires : unit.descriptor().requires()) {
              var dependency = project.unit(unit.realm().name(), requires.name()).orElse(null);
              if (dependency == null) continue;
              lines.add(indent + "<dependency>");
              lines.add(indent + "  <groupId>" + group + "</groupId>");
              lines.add(indent + "  <artifactId>" + dependency.name() + "</artifactId>");
              lines.add(indent + "  <version>" + project.version(dependency) + "</version>");
              lines.add(indent + "</dependency>");
            }
            continue;
          }
          lines.add(Template.replace(line, binding));
        }
        var pom = pom(unit);
        Files.createDirectories(pom.getParent());
        Files.write(pom, lines);
        bach.getLog().debug("Generated pom: %s", pom);
      }
    }

    String group(String template) {
      var projectGroup = project.deployment().mavenGroup();
      if (projectGroup != null) return projectGroup;
      try {
        projectGroup = substring(template, "<groupId>", "<");
        return projectGroup;
      } catch (Exception e) {
        bach.getLog().warning("Guessing Maven Group Id failed: %s", e);
      }
      throw new AssertionError("Maven GroupId not defined!");
    }

    Path pom(Unit unit) {
      return project.folder().realm(unit.realm().name(), "maven", unit.name(), "pom.xml");
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
        if (Files.isRegularFile(pom(unit))) {
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
      if (deployment.mavenRepositoryId() == null || deployment.mavenUri() == null) {
        // log("No Maven deployment record available.");
        return;
      }
      for (var type : ScriptType.values()) {
        generateMavenDeployScript(type, deployment, units);
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
      var pom = "pomFile=" + type.quote(pom(unit));
      var file = "file=" + type.quote(project.modularJar(unit));
      var sources = "sources=" + type.quote(project.sourcesJar(unit));
      var javadoc = "javadoc=" + type.quote(project.javadocJar(unit.realm()));
      return String.join(" ", "-D" + pom, "-D" + file, "-D" + sources, "-D" + javadoc);
    }
  }

  /** Convert all {@link String}-based properties in an instance of {@code Map<String, String>}. */
  private static Map<String, String> map(Properties properties) {
    var map = new HashMap<String, String>();
    for (var name : properties.stringPropertyNames()) {
      map.put(name, properties.getProperty(name));
    }
    return Map.copyOf(map);
  }

  /** Extract substring between begin and end tags. */
  private static String substring(String string, String beginTag, String endTag) {
    int beginIndex = string.indexOf(beginTag) + beginTag.length();
    int endIndex = string.indexOf(endTag, beginIndex);
    return string.substring(beginIndex, endIndex).trim();
  }
}
