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

import static java.util.stream.Collectors.toList;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*BODY*/
/** Load required modules. */
public /*STATIC*/ class Library {

  public static void main(String... args) {
    System.out.println("Library.main(" + List.of(args) + ")");
    System.out.println("  requires -> " + RequiresMap.of(args));
    var modulePath = new ModulePath(Path.of("demo/lib"));
    System.out.println("--module-path=" + List.of(modulePath.paths));
    System.out.println("  modules  -> " + modulePath.modules());
    System.out.println("  requires -> " + modulePath.requires());
    var sourcePath = new SourcePath(Path.of("demo/src"));
    System.out.println("--module-source-path=" + List.of(sourcePath.paths));
    System.out.println("  modules  -> " + sourcePath.modules());
    System.out.println("  requires -> " + sourcePath.requires());
  }

  static class RequiresMap extends TreeMap<String, Set<Version>> {

    static RequiresMap of(String... strings) {
      var map = new RequiresMap();
      for (var string : strings) {
        var versionMarkerIndex = string.indexOf('@');
        var any = versionMarkerIndex == -1;
        var module = any ? string : string.substring(0, versionMarkerIndex);
        var version = any ? null : Version.parse(string.substring(versionMarkerIndex + 1));
        map.merge(module, any ? Set.of() : Set.of(version));
      }
      return map;
    }

    static <E> Stream<E> merge(Set<E> set1, Set<E> set2) {
      return Stream.concat(set1.stream(), set2.stream()).distinct();
    }

    void merge(ModuleDescriptor.Requires requires) {
      merge(requires.name(), requires.compiledVersion().map(Set::of).orElse(Set.of()));
    }

    void merge(String key, String version) {
      merge(key, version.isEmpty() ? Set.of() : Set.of(Version.parse(version)));
    }

    void merge(String key, Set<Version> value) {
      merge(key, value, (oldSet, newSet) -> Set.of(merge(oldSet, newSet).toArray(Version[]::new)));
    }

    RequiresMap validate() {
      var invalids = entrySet().stream().filter(e -> e.getValue().size() > 1).collect(toList());
      if (invalids.isEmpty()) {
        return this;
      }
      throw new IllegalStateException("Multiple versions mapped: " + invalids);
    }
  }

  static class ModulePath {

    final Path[] paths;

    ModulePath(Path... paths) {
      this.paths = paths;
    }

    Stream<ModuleDescriptor> findAll() {
      return ModuleFinder.of(paths).findAll().stream().map(ModuleReference::descriptor);
    }

    public Set<String> modules() {
      return findAll().map(ModuleDescriptor::name).collect(Collectors.toCollection(TreeSet::new));
    }

    public RequiresMap requires() {
      var map = new RequiresMap();
      findAll().map(ModuleDescriptor::requires).flatMap(Set::stream).forEach(map::merge);
      return map.validate();
    }
  }

  static class SourcePath {

    final Path[] paths;

    SourcePath(Path... paths) {
      this.paths = paths;
    }

    public Set<String> modules() {
      return Set.of();
    }

    public RequiresMap requires() {
      return new RequiresMap();
    }
  }
}
