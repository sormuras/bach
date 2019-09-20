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

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/*BODY*/
/** Modular project model. */
public /*STATIC*/ class Project {

  /** Base directory. */
  public final Path baseDirectory;
  /** Target directory. */
  public final Path targetDirectory;
  /** Name of the project. */
  public final String name;
  /** Version of the project. */
  public final Version version;
  /** Library. */
  public final Library library;
  /** Realms. */
  public final List<Realm> realms;

  public Project(
      Path baseDirectory,
      Path targetDirectory,
      String name,
      Version version,
      Library library,
      List<Realm> realms) {
    this.baseDirectory = Util.requireNonNull(baseDirectory, "base");
    this.targetDirectory = Util.requireNonNull(targetDirectory, "targetDirectory");
    this.version = Util.requireNonNull(version, "version");
    this.name = Util.requireNonNull(name, "name");
    this.library = library;
    this.realms = List.copyOf(Util.requireNonEmpty(realms, "realms"));
  }

  /** Manage external 3rd-party modules. */
  public static class Library {
    /** List of library paths to external 3rd-party modules. */
    public final List<Path> modulePaths;
    /** Map external 3rd-party module names to their {@code URI}s. */
    public final Function<String, URI> moduleMapper;

    public Library(List<Path> modulePaths, Function<String, URI> moduleMapper) {
      this.modulePaths = List.copyOf(Util.requireNonEmpty(modulePaths, "modulePaths"));
      this.moduleMapper = moduleMapper;
    }
  }

  /** Java module source unit. */
  public static class Module {
    /** Path to the backing {@code module-info.java} file. */
    public final Path info;
    /** Paths to the resources directories. */
    public final List<Path> sources;
    /** Paths to the resources directories. */
    public final List<Path> resources;
    /** Associated module descriptor, normally parsed from module {@link #info} file. */
    public final ModuleDescriptor descriptor;

    public Module(
        Path info, List<Path> sources, List<Path> resources, ModuleDescriptor descriptor) {
      this.info = info;
      this.sources = List.copyOf(sources);
      this.resources = List.copyOf(resources);
      this.descriptor = descriptor;
    }
  }

  /** Main- and test realms. */
  public static class Realm {
    /** Name of the realm. */
    public final String name;
    /** Module source path specifies where to find input source files for multiple modules. */
    public final String moduleSourcePath;
    /** Map of declared module source unit. */
    public final Map<String, Module> modules;
    /** List of required realms. */
    public final List<Realm> realms;

    public Realm(
        String name, String moduleSourcePath, Map<String, Module> modules, Realm... realms) {
      this.name = name;
      this.moduleSourcePath = moduleSourcePath;
      this.modules = Map.copyOf(modules);
      this.realms = List.of(realms);
    }
  }
}
