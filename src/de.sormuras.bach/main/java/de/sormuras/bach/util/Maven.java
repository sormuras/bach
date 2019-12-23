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
import de.sormuras.bach.Call;
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

    final Bach bach;
    final Project project;

    public Scribe(Bach bach) {
      this.bach = bach;
      this.project = bach.getProject();
    }

    public void generateMavenPoms(Iterable<Unit> units) throws Exception {
      var projectPom = project.folder().src(Bach.Default.MAVEN_POM_TEMPLATE);
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
        var command =
            new Call("Maven Command Properties")
                .add("-D" + "file=" + project.modularJar(unit))
                .add("-D" + "pomFile=" + pom)
                .add("-D" + "sources=" + project.sourcesJar(unit))
                .add("-D" + "javadoc=" + project.javadocJar(unit.realm()));
        var cli = project.folder().deploy(unit, Bach.Default.MAVEN_CLI_ARGUMENTS);
        Files.write(cli, command.toList(false));
      }
    }

    String group(String template) {
      var projectGroup = project.group();
      if (projectGroup != null) return projectGroup;
      try {
        return substring(template, "<groupId>", "<");
      } catch (Exception e) {
        bach.getLog().warning("Guessing Maven Group Id failed: %s", e);
      }
      throw new AssertionError("Maven GroupId not defined!");
    }

    Path pom(Unit unit) {
      return project.folder().deploy(unit, "pom.xml");
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
