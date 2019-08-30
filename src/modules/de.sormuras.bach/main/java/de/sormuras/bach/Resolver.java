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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.SourceVersion;

/*BODY*/
/** Resolves required modules. */
/*STATIC*/ class Resolver {

  static void resolve(Bach bach, String... args) {
    var library = Group.parseModulePath(bach.configuration.getLibraryPaths());
    bach.out.println("Library of -> " + bach.configuration.getSourceDirectories());
    bach.out.println("  modules  -> " + library.modules);
    bach.out.println("  requires -> " + library.requires);

    var sources = Group.parseSourcePath(bach.configuration.getSourceDirectories());
    bach.out.println("Sources of -> " + bach.configuration.getSourceDirectories());
    bach.out.println("  modules  -> " + sources.modules);
    bach.out.println("  requires -> " + sources.requires);
  }

  static class Group {

    static Group parseStrings(List<String> declaredModules, List<String> requires) {
      var map = new TreeMap<String, Set<Version>>();
      for (var string : requires) {
        var versionMarkerIndex = string.indexOf('@');
        var any = versionMarkerIndex == -1;
        var module = any ? string : string.substring(0, versionMarkerIndex);
        var version = any ? null : Version.parse(string.substring(versionMarkerIndex + 1));
        map.merge(module, any ? Set.of() : Set.of(version), Util::concat);
      }
      return new Group(new TreeSet<>(declaredModules), map);
    }

    static final Pattern REQUIRES_PATTERN =
        Pattern.compile(
            "(?:requires)" // key word
                + "(?:\\s+[\\w.]+)?" // optional modifiers
                + "\\s+([\\w.]+)" // module name
                + "(?:\\s*/\\*\\s*([\\w.\\-+]+)\\s*\\*/\\s*)?" // optional '/*' version '*/'
                + ";"); // end marker

    static Group parseModulePath(Collection<Path> modulePath) {
      var declaredModules = new TreeSet<String>();
      var map = new TreeMap<String, Set<Version>>();
      findAll(modulePath.toArray(Path[]::new))
          .peek(descriptor -> declaredModules.add(descriptor.name()))
          .map(ModuleDescriptor::requires)
          .flatMap(Set::stream)
          .distinct()
          .forEach(requires -> merge(map, requires));
      return new Group(declaredModules, map);
    }

    static void merge(Map<String, Set<Version>> map, ModuleDescriptor.Requires requires) {
      var key = requires.name();
      var value = requires.compiledVersion().map(Set::of).orElse(Set.of());
      map.merge(key, value, Util::concat);
    }

    static Stream<ModuleDescriptor> findAll(Path... paths) {
      return ModuleFinder.of(paths).findAll().stream().map(ModuleReference::descriptor);
    }

    static Group parseSourcePath(Iterable<Path> sourcePaths) {
      var declaredModules = new TreeSet<String>();
      var map = new TreeMap<String, Set<Version>>();
      for (var sourcePath : sourcePaths) {
        try (var directoryStream = Files.list(sourcePath)) {
          var directories =
              directoryStream
                  .filter(Files::isDirectory)
                  .filter(path -> SourceVersion.isName(path.getFileName().toString()))
                  .collect(Collectors.toList());
          directories.stream()
              .map(Path::getFileName)
              .map(Path::toString)
              .forEach(declaredModules::add);
          for (var root : directories)
            try (var stream = Files.find(root, 9, (p, __) -> Util.isModuleInfo(p))) {
              for (var moduleInfo : stream.collect(Collectors.toSet())) {
                var source = Files.readString(moduleInfo);
                var requiresMatcher = REQUIRES_PATTERN.matcher(source);
                while (requiresMatcher.find()) {
                  var requiredName = requiresMatcher.group(1);
                  var requiredVersion = requiresMatcher.group(2);
                  if (requiredVersion == null) {
                    map.putIfAbsent(requiredName, Set.of());
                    continue;
                  }
                  map.merge(requiredName, Set.of(Version.parse(requiredVersion)), Util::concat);
                }
              }
            }
        } catch (IOException e) {
          throw new UncheckedIOException("list: " + sourcePath, e);
        }
      }
      return new Group(declaredModules, map);
    }


    final Set<String> modules;
    final Map<String, Set<Version>> requires;

    private Group(Set<String> modules, Map<String, Set<Version>> requires) {
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
      if (versions.size() > 1) {
        throw new IllegalStateException("Multiple versions: " + requiredModule + " -> " + versions);
      }
      return versions.stream().findFirst();
    }
  }
}
