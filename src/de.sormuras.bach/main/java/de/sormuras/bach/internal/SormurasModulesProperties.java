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
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

/** https://github.com/sormuras/modules */
public class SormurasModulesProperties {

  private Map<String, String> moduleMaven;
  private Map<String, String> moduleVersion;
  private final Map<String, String> variants;

  public SormurasModulesProperties(Map<String, String> variants) {
    this.variants = variants;
  }

  public Optional<String> lookup(String module) {
    if (moduleMaven == null && moduleVersion == null)
      try {
        // TODO var http = project.library().supplier().get();
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
    if (maven == null) return Optional.empty();
    var indexOfColon = maven.indexOf(':');
    if (indexOfColon < 0) throw new AssertionError("Expected group:artifact, but got: " + maven);
    var version = variants.getOrDefault(module, moduleVersion.get(module));
    if (version == null) return Optional.empty();
    var group = maven.substring(0, indexOfColon);
    var artifact = maven.substring(indexOfColon + 1);
    return Optional.of(Maven.Joiner.of(group, artifact, version).toString());
  }

  private static final String ROOT = "https://github.com/sormuras/modules";

  private static Map<String, String> load(Resources resources, String properties) throws Exception {
    var root = Path.of(System.getProperty("user.home", ""));
    var cache = Files.createDirectories(root.resolve(".bach/modules"));
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
