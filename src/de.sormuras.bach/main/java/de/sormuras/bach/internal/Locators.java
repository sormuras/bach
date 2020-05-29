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

import de.sormuras.bach.Bach;
import de.sormuras.bach.Project;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

/** {@link Project.Locator}-related utilities. */
public /*static*/ class Locators {

  public static class ComposedLocator implements Project.Locator {

    private final List<Project.Locator> locators;

    public ComposedLocator(Collection<? extends Project.Locator> locators) {
      this.locators = List.copyOf(locators);
    }

    @Override
    public void accept(Bach bach) {
      locators.forEach(locator -> locator.accept(bach));
    }

    @Override
    public Optional<String> locate(String module) {
      return locators.stream().flatMap(locator -> locator.locate(module).stream()).findFirst();
    }
  }

  public static class MavenLocator implements Project.Locator {

    private final String repository;
    private final Map<String, String> coordinates;

    public MavenLocator(Map<String, String> coordinates) {
      this(Maven.CENTRAL_REPOSITORY, coordinates);
    }

    public MavenLocator(String repository, Map<String, String> coordinates) {
      this.repository = repository;
      this.coordinates = coordinates;
    }

    @Override
    public Optional<String> locate(String module) {
      var coordinate = coordinates.get(module);
      if (coordinate == null) return Optional.empty();
      var split = coordinate.split(":");
      if (split.length < 3) return Optional.of(coordinate);
      var group = split[0];
      var artifact = split[1];
      var version = split[2];
      var joiner = new Maven.Joiner().repository(repository);
      joiner.group(group).artifact(artifact).version(version);
      joiner.classifier(split.length < 4 ? "" : split[3]);
      return Optional.of(joiner.toString());
    }
  }

  /** https://github.com/sormuras/modules */
  public static class SormurasModulesLocator implements Project.Locator {

    private final Map<String, String> versions;
    private Bach bach;
    private Map<String, String> moduleMaven;
    private Map<String, String> moduleVersion;

    public SormurasModulesLocator(Map<String, String> versions) {
      this.versions = versions;
    }

    @Override
    public void accept(Bach bach) {
      this.bach = bach;
    }

    @Override
    public Optional<String> locate(String module) {
      if (moduleMaven == null && moduleVersion == null)
        try {
          if (bach == null) throw new IllegalStateException("Bach field not set");
          var resources = new Resources(bach.getHttpClient());
          moduleMaven = load(resources, "module-maven.properties");
          moduleVersion = load(resources, "module-version.properties");
        } catch (Exception e) {
          throw new RuntimeException("Load module properties failed", e);
        }
      if (moduleMaven == null) throw new IllegalStateException("Map module-maven is null");
      if (moduleVersion == null) throw new IllegalStateException("Map module-version is null");

      var maven = moduleMaven.get(module);
      if (maven == null) return Optional.empty();
      var indexOfColon = maven.indexOf(':');
      if (indexOfColon < 0) throw new AssertionError("Expected group:artifact, but got: " + maven);
      var version = versions.getOrDefault(module, moduleVersion.get(module));
      if (version == null) return Optional.empty();
      var group = maven.substring(0, indexOfColon);
      var artifact = maven.substring(indexOfColon + 1);
      return Optional.of(Maven.Joiner.of(group, artifact, version).toString());
    }

    private static final String ROOT = "https://github.com/sormuras/modules";

    private static Map<String, String> load(Resources resources, String properties)
        throws Exception {
      var user = Path.of(System.getProperty("user.home"));
      var cache = Files.createDirectories(user.resolve(".bach/modules"));
      var source = URI.create(String.join("/", ROOT, "raw/master", properties));
      var target = cache.resolve(properties);
      var path = resources.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
      return map(load(new Properties(), path));
    }

    /** Load all strings from the specified file into the passed properties instance. */
    private static Properties load(Properties properties, Path path) {
      if (Files.isRegularFile(path)) {
        try (var reader = Files.newBufferedReader(path)) {
          properties.load(reader);
        } catch (Exception e) {
          throw new RuntimeException("Load properties failed: " + path, e);
        }
      }
      return properties;
    }

    /** Convert all {@link String}-based properties into a {@code Map<String, String>}. */
    private static Map<String, String> map(Properties properties) {
      var map = new TreeMap<String, String>();
      for (var name : properties.stringPropertyNames()) {
        map.put(name, properties.getProperty(name));
      }
      return map;
    }
  }

  private Locators() {}
}
