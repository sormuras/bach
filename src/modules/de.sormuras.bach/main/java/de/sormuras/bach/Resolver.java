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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.SourceVersion;

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

  public void resolve() {
    var entries = project.library.modulePaths.toArray(Path[]::new);
    var library = scan(ModuleFinder.of(entries));
    bach.log("Library of -> %s", project.library.modulePaths);
    bach.log("  modules  -> " + library.modules);
    bach.log("  requires -> " + library.requires);

    var units = new ArrayList<Path>();
    for (var realm : project.realms) {
      for (var unit : realm.units.values()) {
        units.add(unit.info);
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

    var downloader = new Util.Downloader(bach.out, bach.err);
    var worker = new Scanner.Worker(project, downloader);
    do {
      bach.log("Loading missing modules: %s", missing);
      var items = new ArrayList<Util.Downloader.Item>();
      for (var entry : missing.entrySet()) {
        var module = entry.getKey();
        var versions = entry.getValue();
        items.add(worker.toTransferItem(module, versions));
      }
      var lib = project.library.modulePaths.get(0);
      downloader.download(lib, items);
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
        throw new NoSuchElementException("Module " + requiredModule + " is not mapped");
      }
      if (versions.size() > 1) {
        throw new IllegalStateException("Multiple versions: " + requiredModule + " -> " + versions);
      }
      return versions.stream().findFirst();
    }

    static class Worker {

      static class Lookup {

        final String name;
        final Properties properties;
        final Set<Pattern> patterns;
        final UnaryOperator<String> custom;

        Lookup(Util.Downloader downloader, Path lib, String name, UnaryOperator<String> custom) {
          this.name = name;
          var uri = "https://github.com/sormuras/modules/raw/master/" + name;
          var modules = Path.of(System.getProperty("user.home")).resolve(".bach/modules");
          try {
            Files.createDirectories(modules);
          } catch (IOException e) {
            throw new UncheckedIOException("Creating directories failed: " + modules, e);
          }
          var defaultModules = downloader.download(URI.create(uri), modules.resolve(name));
          var defaults = Util.load(new Properties(), defaultModules);
          this.properties = Util.load(new Properties(defaults), lib.resolve(name));
          this.patterns =
              properties.keySet().stream()
                  .map(Object::toString)
                  .filter(key -> !SourceVersion.isName(key))
                  .map(Pattern::compile)
                  .collect(Collectors.toSet());
          this.custom = custom;
        }

        String get(String key) {
          try {
            return custom.apply(key);
          } catch (UnmappedModuleException e) {
            // fall-through
          }
          var value = properties.getProperty(key);
          if (value != null) {
            return value;
          }
          for (var pattern : patterns) {
            if (pattern.matcher(key).matches()) {
              return properties.getProperty(pattern.pattern());
            }
          }
          throw new IllegalStateException("No lookup value mapped for: " + key);
        }

        @Override
        public String toString() {
          var size = properties.size();
          var names = properties.stringPropertyNames().size();
          return String.format(
              "module properties {name: %s, size: %d, names: %d}", name, size, names);
        }
      }

      final Project project;
      final Properties moduleUri;
      final Lookup moduleMaven, moduleVersion;

      Worker(Project project, Util.Downloader transfer) {
        this.project = project;
        var lib = project.library.modulePaths.get(0);
        this.moduleUri = Util.load(new Properties(), lib.resolve("module-uri.properties"));
        this.moduleMaven =
            new Lookup(
                transfer,
                lib,
                "module-maven.properties",
                project.library.mavenGroupColonArtifactMapper);
        this.moduleVersion =
            new Lookup(
                transfer, lib, "module-version.properties", project.library.mavenVersionMapper);
      }

      private URI getModuleUri(String module) {
        try {
          return project.library.moduleMapper.apply(module);
        } catch (UnmappedModuleException e) {
          var uri = moduleUri.getProperty(module);
          if (uri == null) {
            return null;
          }
          return URI.create(uri);
        }
      }

      Util.Downloader.Item toTransferItem(String module, Set<Version> set) {
        var uri = getModuleUri(module);
        if (uri != null) {
          var file = Util.findFileName(uri);
          var version = Util.findVersion(file.orElse(""));
          return Util.Downloader.Item.of(
              uri, module + version.map(v -> '-' + v).orElse("") + ".jar");
        }
        var repository = project.library.mavenRepositoryMapper.apply(module);
        var maven = moduleMaven.get(module).split(":");
        var group = maven[0];
        var artifact = maven[1];
        var version = Util.singleton(set).map(Object::toString).orElse(moduleVersion.get(module));
        var mappedUri = toUri(repository.toString(), group, artifact, version);
        return Util.Downloader.Item.of(mappedUri, module + '-' + version + ".jar");
      }

      private URI toUri(String repository, String group, String artifact, String version) {
        var file = artifact + '-' + version + ".jar";
        var uri = String.join("/", repository, group.replace('.', '/'), artifact, version, file);
        return URI.create(uri);
      }
    }
  }
}
