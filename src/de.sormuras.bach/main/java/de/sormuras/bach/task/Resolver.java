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

package de.sormuras.bach.task;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Log;
import de.sormuras.bach.project.Library;
import de.sormuras.bach.util.Maven;
import de.sormuras.bach.util.Uris;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.lang.module.ModuleDescriptor.Version;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

class Resolver {

  private final Log log;
  private final Path lib;
  private final Library library;

  private final Uris uris;
  private final Maven maven;

  Resolver(Bach bach) throws Exception {
    this.log = bach.getLog();
    var project = bach.getProject();
    this.lib = project.folder().lib();
    this.library = project.structure().library();

    var http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    this.uris = new Uris(log, http);
    this.maven = createMaven(uris);
  }

  void loadMissingModules(Map<String, Set<Version>> missing) throws Exception {
    log.debug("Loading modules: %s", missing);
    for (var entry : missing.entrySet()) {
      var module = entry.getKey();
      var mapped = library.moduleMapper().apply(module);
      if (mapped != null) {
        var jar = lib.resolve(module + ".jar");
        uris.copy(mapped, jar, StandardCopyOption.COPY_ATTRIBUTES);
        continue;
      }
      var versions = entry.getValue();
      var version = singleton(versions).map(Object::toString).orElse(maven.version(module));
      var ga = maven.lookup(module, version).split(":");
      var group = ga[0];
      var artifact = ga[1];
      var repository = library.mavenRepositoryMapper().apply(group, version);
      uris.copy(
          maven.toUri(repository, group, artifact, version),
          lib.resolve(module + '-' + version + ".jar"),
          StandardCopyOption.COPY_ATTRIBUTES);
    }
  }

  Maven createMaven(Uris uris) throws Exception {
    var user = Path.of(System.getProperty("user.home"));
    var cache = Files.createDirectories(user.resolve(".bach/modules"));
    var artifactPath =
        uris.copy(
            URI.create("https://github.com/sormuras/modules/raw/master/module-maven.properties"),
            cache.resolve("module-maven.properties"),
            StandardCopyOption.COPY_ATTRIBUTES);
    var artifactMap = map(load(new Properties(), artifactPath));
    var artifactLookup = new Maven.Lookup(library.mavenGroupColonArtifactMapper(), artifactMap);

    var versionPath =
        uris.copy(
            URI.create("https://github.com/sormuras/modules/raw/master/module-version.properties"),
            cache.resolve("module-version.properties"),
            StandardCopyOption.COPY_ATTRIBUTES);
    var versionMap = map(load(new Properties(), versionPath));
    var versionLookup = new Maven.Lookup(library.mavenVersionMapper(), versionMap);
    return new Maven(log, uris, artifactLookup, versionLookup);
  }

  private static Properties load(Properties properties, Path path) {
    if (Files.isRegularFile(path)) {
      try (var reader = Files.newBufferedReader(path)) {
        properties.load(reader);
      } catch (Exception e) {
        throw new RuntimeException("Reading properties failed: " + path, e);
      }
    }
    return properties;
  }

  /** Convert all {@link String}-based properties in an instance of {@code Map<String, String>}. */
  private static Map<String, String> map(Properties properties) {
    var map = new HashMap<String, String>();
    for (var name : properties.stringPropertyNames()) {
      map.put(name, properties.getProperty(name));
    }
    return Map.copyOf(map);
  }

  private static <T> Optional<T> singleton(Collection<T> collection) {
    if (collection.isEmpty()) {
      return Optional.empty();
    }
    if (collection.size() != 1) {
      throw new IllegalStateException("Too many elements: " + collection);
    }
    return Optional.of(collection.iterator().next());
  }
}
