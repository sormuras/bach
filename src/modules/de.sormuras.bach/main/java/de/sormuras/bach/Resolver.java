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
import javax.lang.model.SourceVersion;

/*BODY*/
/** Resolves required modules. */
/*STATIC*/ class Resolver {

  private static final Pattern MODULE_NAME_PATTERN = Pattern.compile("(?:module)\\s+([\\w.]+)");
  private static final Pattern MODULE_REQUIRES_PATTERN =
      Pattern.compile(
          "(?:requires)" // key word
              + "(?:\\s+[\\w.]+)?" // optional modifiers
              + "\\s+([\\w.]+)" // module name
              + "(?:\\s*/\\*\\s*([\\w.\\-+]+)\\s*\\*/\\s*)?" // optional '/*' version '*/'
              + ";"); // end marker

  static void resolve(Bach bach) {
    var entries = bach.configuration.getLibraryPaths().toArray(Path[]::new);
    var library = of(ModuleFinder.of(entries));
    bach.out.println("Library of -> " + bach.configuration.getLibraryPaths());
    bach.out.println("  modules  -> " + library.modules);
    bach.out.println("  requires -> " + library.requires);

    var sources = of(bach.configuration.getSourceDirectories());
    bach.out.println("Sources of -> " + bach.configuration.getSourceDirectories());
    bach.out.println("  modules  -> " + sources.modules);
    bach.out.println("  requires -> " + sources.requires);

    var systems = of(ModuleFinder.ofSystem());

    var missing = new TreeMap<String, Set<Version>>();
    missing.putAll(sources.requires);
    missing.putAll(library.requires);
    sources.getDeclaredModules().forEach(missing::remove);
    library.getDeclaredModules().forEach(missing::remove);
    systems.getDeclaredModules().forEach(missing::remove);
    if (missing.isEmpty()) {
      return;
    }

    var transfer = new Transfer(bach.out, bach.err);
    var worker = new Worker(bach, transfer);
    do {
      bach.out.println("Loading missing modules: " + missing);
      var items = new ArrayList<Transfer.Item>();
      for (var entry : missing.entrySet()) {
        var module = entry.getKey();
        var versions = entry.getValue();
        items.add(worker.toTransferItem(module, versions));
      }
      var lib = bach.configuration.getLibraryDirectory();
      transfer.getFiles(lib, items);
      library = of(ModuleFinder.of(entries));
      missing = new TreeMap<>(library.requires);
      library.getDeclaredModules().forEach(missing::remove);
      systems.getDeclaredModules().forEach(missing::remove);
    } while (!missing.isEmpty());
  }

  /** Command-line argument factory. */
  static Resolver of(Collection<String> declaredModules, Iterable<String> requires) {
    var map = new TreeMap<String, Set<Version>>();
    for (var string : requires) {
      var versionMarkerIndex = string.indexOf('@');
      var any = versionMarkerIndex == -1;
      var module = any ? string : string.substring(0, versionMarkerIndex);
      var version = any ? null : Version.parse(string.substring(versionMarkerIndex + 1));
      map.merge(module, any ? Set.of() : Set.of(version), Util::concat);
    }
    return new Resolver(new TreeSet<>(declaredModules), map);
  }

  static Resolver of(ModuleFinder finder) {
    var declaredModules = new TreeSet<String>();
    var requiredModules = new TreeMap<String, Set<Version>>();
    finder.findAll().stream()
        .map(ModuleReference::descriptor)
        .peek(descriptor -> declaredModules.add(descriptor.name()))
        .map(ModuleDescriptor::requires)
        .flatMap(Set::stream)
        .filter(r -> !r.modifiers().contains(ModuleDescriptor.Requires.Modifier.MANDATED))
        .filter(r -> !r.modifiers().contains(ModuleDescriptor.Requires.Modifier.STATIC))
        .distinct()
        .forEach(
            requires ->
                requiredModules.merge(
                    requires.name(),
                    requires.compiledVersion().map(Set::of).orElse(Set.of()),
                    Util::concat));
    return new Resolver(declaredModules, requiredModules);
  }

  static Resolver of(String... sources) {
    var declaredModules = new TreeSet<String>();
    var map = new TreeMap<String, Set<Version>>();
    for (var source : sources) {
      var nameMatcher = MODULE_NAME_PATTERN.matcher(source);
      if (!nameMatcher.find()) {
        throw new IllegalArgumentException("Expected module-info.java source, but got: " + source);
      }
      declaredModules.add(nameMatcher.group(1).trim());
      var requiresMatcher = MODULE_REQUIRES_PATTERN.matcher(source);
      while (requiresMatcher.find()) {
        var name = requiresMatcher.group(1);
        var version = requiresMatcher.group(2);
        map.merge(name, version == null ? Set.of() : Set.of(Version.parse(version)), Util::concat);
      }
    }
    return new Resolver(declaredModules, map);
  }

  static Resolver of(Collection<Path> sourcePaths) {
    var sources = new ArrayList<String>();
    for (var sourcePath : sourcePaths) {
      try (var stream = Files.find(sourcePath, 9, (p, __) -> Util.isModuleInfo(p))) {
        for (var moduleInfo : stream.collect(Collectors.toSet())) {
          sources.add(Files.readString(moduleInfo));
        }
      } catch (IOException e) {
        throw new UncheckedIOException("find or read failed: " + sourcePath, e);
      }
    }
    return of(sources.toArray(new String[0]));
  }

  private final Set<String> modules;
  private final Map<String, Set<Version>> requires;

  Resolver(Set<String> modules, Map<String, Set<Version>> requires) {
    this.modules = modules;
    this.requires = requires;
  }

  Set<String> getDeclaredModules() {
    return modules;
  }

  Set<String> getRequiredModules() {
    return requires.keySet();
  }

  Optional<Version> getRequiredVersion(String requiredModule) {
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

      Lookup(Transfer transfer, Path lib, String name, UnaryOperator<String> custom) {
        this.name = name;
        var uri = "https://github.com/sormuras/modules/raw/master/" + name;
        var modules = Path.of(System.getProperty("user.home")).resolve(".bach/modules");
        try {
          Files.createDirectories(modules);
        } catch (IOException e) {
          throw new UncheckedIOException("Creating directories failed: " + modules, e);
        }
        var defaultModules = transfer.getFile(URI.create(uri), modules.resolve(name));
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
        } catch (Configuration.UnmappedModuleException e) {
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

    final Bach bach;
    final Properties moduleUri;
    final Lookup moduleMaven, moduleVersion;

    Worker(Bach bach, Transfer transfer) {
      this.bach = bach;
      var cfg = bach.configuration;
      var lib = cfg.getLibraryDirectory();
      this.moduleUri = Util.load(new Properties(), lib.resolve("module-uri.properties"));
      this.moduleMaven =
          new Lookup(transfer, lib, "module-maven.properties", cfg::getModuleMavenGroupAndArtifact);
      this.moduleVersion =
          new Lookup(transfer, lib, "module-version.properties", cfg::getModuleVersion);
    }

    private URI getModuleUri(String module) {
      try {
        return bach.configuration.getModuleUri(module);
      } catch (Configuration.UnmappedModuleException e) {
        var uri = moduleUri.getProperty(module);
        if (uri == null) {
          return null;
        }
        return URI.create(uri);
      }
    }

    private Transfer.Item toTransferItem(String module, Set<Version> set) {
      var uri = getModuleUri(module);
      if (uri != null) {
        var file = Util.findFileName(uri);
        var version = Util.findVersion(file.orElse(""));
        return Transfer.Item.of(uri, module + version.map(v -> '-' + v).orElse("") + ".jar");
      }
      var maven = moduleMaven.get(module).split(":");
      var group = maven[0];
      var artifact = maven[1];
      var version = Util.singleton(set).map(Object::toString).orElse(moduleVersion.get(module));
      return Transfer.Item.of(toUri(group, artifact, version), module + '-' + version + ".jar");
    }

    private URI toUri(String group, String artifact, String version) {
      var host = "https://repo1.maven.org/maven2";
      var file = artifact + '-' + version + ".jar";
      var uri = String.join("/", host, group.replace('.', '/'), artifact, version, file);
      return URI.create(uri);
    }
  }
}
