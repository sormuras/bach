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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

/*BODY*/
/** 3rd-party module resolver. */
public /*STATIC*/ class Resolver {

  /** Command-line argument factory. */
  public static Scanner scan(Collection<String> declaredModules, Iterable<String> requires) {
    var map = new TreeMap<String, Set<Version>>();
    for (var string : requires) {
      var versionMarkerIndex = string.indexOf('@');
      var any = versionMarkerIndex == -1;
      var module = any ? string : string.substring(0, versionMarkerIndex);
      var version = any ? null : Version.parse(string.substring(versionMarkerIndex + 1));
      map.merge(module, any ? Set.of() : Set.of(version), Util::concat);
    }
    return new Scanner(new TreeSet<>(declaredModules), map);
  }

  public static Scanner scan(ModuleFinder finder) {
    var declaredModules = new TreeSet<String>();
    var requiredModules = new TreeMap<String, Set<Version>>();
    var stream =
        finder.findAll().stream()
            .map(ModuleReference::descriptor)
            .peek(descriptor -> declaredModules.add(descriptor.name()))
            .map(ModuleDescriptor::requires)
            .flatMap(Set::stream)
            .filter(r -> !r.modifiers().contains(Requires.Modifier.STATIC));
    merge(requiredModules, stream);
    return new Scanner(declaredModules, requiredModules);
  }

  public static Scanner scan(String... sources) {
    var declaredModules = new TreeSet<String>();
    var requiredModules = new TreeMap<String, Set<Version>>();
    for (var source : sources) {
      var descriptor = Modules.describe(source);
      declaredModules.add(descriptor.name());
      merge(requiredModules, descriptor.requires().stream());
    }
    return new Scanner(declaredModules, requiredModules);
  }

  private static void merge(Map<String, Set<Version>> requiredModules, Stream<Requires> stream) {
    stream
        .filter(requires -> !requires.modifiers().contains(Requires.Modifier.MANDATED))
        .forEach(
            requires ->
                requiredModules.merge(
                    requires.name(),
                    requires.compiledVersion().map(Set::of).orElse(Set.of()),
                    Util::concat));
  }

  public static Scanner scan(Collection<Path> paths) {
    var sources = new ArrayList<String>();
    for (var path : paths) {
      if (Files.isDirectory(path)) {
        path = path.resolve("module-info.java");
      }
      try {
        sources.add(Files.readString(path));
      } catch (IOException e) {
        throw new UncheckedIOException("find or read failed: " + path, e);
      }
    }
    return scan(sources.toArray(new String[0]));
  }

  private final Bach bach;
  private final Project project;

  Resolver(Bach bach) {
    this.bach = bach;
    this.project = bach.project;
  }

  public void resolve() throws Exception {
    var entries = project.library.modulePaths.toArray(Path[]::new);
    var library = scan(ModuleFinder.of(entries));
    bach.log("Library of -> %s", project.library.modulePaths);
    bach.log("  modules  -> " + library.modules);
    bach.log("  requires -> " + library.requires);

    var units = new ArrayList<Path>();
    for (var realm : project.realms) {
      for (var unit : realm.units) {
        units.add(unit.info.path);
      }
    }
    var sources = scan(units);
    bach.log("Sources of -> %s", units);
    bach.log("  modules  -> " + sources.modules);
    bach.log("  requires -> " + sources.requires);

    var systems = scan(ModuleFinder.ofSystem());
    bach.log("System contains %d modules.", systems.modules.size());

    var missing = new TreeMap<String, Set<Version>>();
    missing.putAll(sources.requires);
    missing.putAll(library.requires);
    sources.getDeclaredModules().forEach(missing::remove);
    library.getDeclaredModules().forEach(missing::remove);
    systems.getDeclaredModules().forEach(missing::remove);
    if (missing.isEmpty()) {
      return;
    }

    var lib = project.library.modulePaths.get(0);
    var uris = Util.load(new Properties(), lib.resolve("module-uri.properties"));
    var http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    var log = new Log(bach.out, bach.err, bach.verbose());
    var resources = new Resources(log, http);

    var cache =
        Files.createDirectories(Path.of(System.getProperty("user.home")).resolve(".bach/modules"));
    var artifactPath =
        resources.copy(
            URI.create("https://github.com/sormuras/modules/raw/master/module-maven.properties"),
            cache.resolve("module-maven.properties"),
            StandardCopyOption.COPY_ATTRIBUTES);

    var artifactLookup =
        new Maven.Lookup(
            project.library.mavenGroupColonArtifactMapper,
            Util.map(Util.load(new Properties(), lib.resolve("module-maven.properties"))),
            Util.map(Util.load(new Properties(), artifactPath)));

    var versionPath =
        resources.copy(
            URI.create("https://github.com/sormuras/modules/raw/master/module-version.properties"),
            cache.resolve("module-version.properties"),
            StandardCopyOption.COPY_ATTRIBUTES);

    var versionLookup =
        new Maven.Lookup(
            project.library.mavenVersionMapper,
            Util.map(Util.load(new Properties(), lib.resolve("module-version.properties"))),
            Util.map(Util.load(new Properties(), versionPath)));
    var maven = new Maven(log, resources, artifactLookup, versionLookup);

    do {
      bach.log("Loading missing modules: %s", missing);
      for (var entry : missing.entrySet()) {
        var module = entry.getKey();
        var direct = uris.getProperty(module);
        if (direct != null) {
          var uri = URI.create(direct);
          var jar = lib.resolve(module + ".jar");
          resources.copy(uri, jar, StandardCopyOption.COPY_ATTRIBUTES);
          continue;
        }
        var versions = entry.getValue();
        var version =
            Util.singleton(versions).map(Object::toString).orElse(versionLookup.apply(module));
        var ga = maven.lookup(module, version).split(":");
        var group = ga[0];
        var artifact = ga[1];
        var repository = project.library.mavenRepositoryMapper.apply(group, version);
        resources.copy(
            maven.toUri(repository, group, artifact, version),
            lib.resolve(module + '-' + version + ".jar"),
            StandardCopyOption.COPY_ATTRIBUTES);
      }
      library = scan(ModuleFinder.of(entries));
      missing = new TreeMap<>(library.requires);
      library.getDeclaredModules().forEach(missing::remove);
      systems.getDeclaredModules().forEach(missing::remove);
    } while (!missing.isEmpty());
  }

  /** Module Scanner. */
  public static class Scanner {

    private final Set<String> modules;
    final Map<String, Set<Version>> requires;

    public Scanner(Set<String> modules, Map<String, Set<Version>> requires) {
      this.modules = modules;
      this.requires = requires;
    }

    public Set<String> getDeclaredModules() {
      return modules;
    }

    public Set<String> getRequiredModules() {
      return requires.keySet();
    }

    public Optional<Version> getRequiredVersion(String requiredModule) {
      var versions = requires.get(requiredModule);
      if (versions == null) {
        throw new UnmappedModuleException(requiredModule);
      }
      if (versions.size() > 1) {
        throw new IllegalStateException("Multiple versions: " + requiredModule + " -> " + versions);
      }
      return versions.stream().findFirst();
    }
  }
}
