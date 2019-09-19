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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/*BODY*/
/*STATIC*/ class Realm {

  /** Collect information about a declared module. */
  class Info {
    final Path base;
    final Path path;
    final String module;
    final String moduleDashVersion;
    final Path resources;
    final Path sources;

    Info(Path base, Path path) {
      this.base = base; // "src/modules"
      this.path = path; // "${module}/${realm}/java/module-info.java"
      this.module = path.getName(0).toString();
      this.moduleDashVersion = module + '-' + Realm.this.configuration.getProjectVersion();
      this.resources = base.resolve(module).resolve(Realm.this.name).resolve("resources");
      this.sources = base.resolve(module).resolve(Realm.this.name).resolve("java");
    }

    Path getResources() {
      return resources;
    }

    Optional<String> getMainClass() {
      return Optional.empty();
    }

    Path getModularJar() {
      return getDestination().resolve("modules").resolve(moduleDashVersion + ".jar");
    }

    Path getSourcesJar() {
      return getDestination().resolve("sources").resolve(moduleDashVersion + "-sources.jar");
    }
  }

  private final String name;
  private final Configuration configuration;
  private final Path destination;
  private final String moduleSourcePath;
  private final Map<String, Info> declaredModules;

  Realm(String name, Configuration configuration) {
    this.name = Util.requireNonNull(name, "realm name");
    this.configuration = Util.requireNonNull(configuration, "configuration");
    this.destination = configuration.getWorkspaceDirectory().resolve(name);
    var declaredModules = new TreeMap<String, Info>();
    var moduleSourcePaths = new TreeSet<String>();
    for (var src : configuration.getSourceDirectories()) {
      try (var stream = Files.list(src)) {
        for (var path : stream.collect(Collectors.toList())) {
          var file = path.resolve(name + "/java/module-info.java");
          if (!Util.isModuleInfo(file)) {
            file = path.resolve(name + "/module/module-info.java");
          }
          if (!Util.isModuleInfo(file)) {
            continue;
          }
          var info = new Info(src, src.relativize(path));
          declaredModules.put(info.module, info);
          var offset = path.relativize(file.subpath(0, file.getNameCount() - 1)).toString();
          moduleSourcePaths.add(String.join(File.separator, src.toString(), "*", offset));
        }
      } catch (IOException e) {
        throw new UncheckedIOException("list directory failed: " + src, e);
      }
    }
    this.declaredModules = declaredModules;
    this.moduleSourcePath = String.join(File.pathSeparator, moduleSourcePaths);
  }

  void addModulePatches(Command javac, Collection<String> modules) {}

  Set<String> getDeclaredModules() {
    return declaredModules.keySet();
  }

  Info getDeclaredModuleInfo(String module) {
    return declaredModules.get(module);
  }

  Path getDestination() {
    return destination;
  }

  List<Path> getModulePaths() {
    var paths = new ArrayList<Path>();
    paths.add(getDestination().resolve("modules"));
    paths.addAll(configuration.getLibraryPaths());
    return Util.findExistingDirectories(paths);
  }

  String getModuleSourcePath() {
    return moduleSourcePath;
  }

  String getName() {
    return name;
  }

  List<Path> getRuntimeModulePaths(Path... initialPaths) {
    return getModulePaths();
  }
}
