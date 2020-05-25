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

import de.sormuras.bach.Project;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.StreamSupport;

/** {@link Project.Locator}-related utilities. */
public /*static*/ class Locators {

  public static class ComposedLocator implements Project.Locator {

    private final Iterable<Project.Locator> locators;

    public ComposedLocator(Iterable<Project.Locator> locators) {
      this.locators = locators;
    }

    @Override
    public String apply(String module) {
      return Math.random() <= 0.5 ? withForEach(module) : withStream(module);
    }

    public String withForEach(String module) {
      for (var locator : locators) {
        var uri = locator.apply(module);
        if (uri != null) return uri;
      }
      return null;
    }

    public String withStream(String module) {
      var stream = StreamSupport.stream(locators.spliterator(), false);
      return stream
          .map(locator -> locator.apply(module))
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null);
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
    public String apply(String module) {
      var coordinate = coordinates.get(module);
      if (coordinate == null) return null;
      var split = coordinate.split(":");
      if (split.length < 3) return coordinate;
      var group = split[0];
      var artifact = split[1];
      var version = split[2];
      var joiner = new Maven.Joiner().repository(repository);
      joiner.group(group).artifact(artifact).version(version);
      joiner.classifier(split.length < 4 ? "" : split[3]);
      return joiner.toString();
    }
  }

  /** https://github.com/sormuras/modules */
  public static class SormurasModulesLocator implements Project.Locator {

    private Map<String, String> moduleMaven;
    private Map<String, String> moduleVersion;
    private final Map<String, String> variants;

    public SormurasModulesLocator(Map<String, String> variants) {
      this.variants = variants;
    }

    @Override
    public String apply(String module) {
      if (moduleMaven == null && moduleVersion == null)
        try {
          // TODO var http = bach.getHttpClient();
          var http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
          var resources = new Resources(http);
          moduleMaven = load(resources, "module-maven.properties");
          moduleVersion = load(resources, "module-version.properties");
        } catch (Exception e) {
          throw new RuntimeException("load module properties failed", e);
        }
      if (moduleMaven == null) throw new IllegalStateException("module-maven map is null");
      if (moduleVersion == null) throw new IllegalStateException("module-version map is null");

      var maven = moduleMaven.get(module);
      if (maven == null) return null;
      var indexOfColon = maven.indexOf(':');
      if (indexOfColon < 0) throw new AssertionError("Expected group:artifact, but got: " + maven);
      var version = variants.getOrDefault(module, moduleVersion.get(module));
      if (version == null) return null;
      return new Maven.Joiner()
          .group(maven.substring(0, indexOfColon))
          .artifact(maven.substring(indexOfColon + 1))
          .version(version)
          .toString();
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
