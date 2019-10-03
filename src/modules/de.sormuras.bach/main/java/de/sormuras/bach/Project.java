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
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/*BODY*/
/** Modular project model. */
public /*STATIC*/ class Project {

  /** Create default project parsing the passed base directory. */
  public static Project of(Path base) {
    if (!Files.isDirectory(base)) {
      throw new IllegalArgumentException("Expected a directory but got: " + base);
    }
    var main = new Realm("main", false, 0, "src/*/main/java", ToolArguments.of(), List.of());
    var name = Optional.ofNullable(base.toAbsolutePath().getFileName());
    return new Project(
        base,
        base.resolve("bin"),
        name.orElse(Path.of("project")).toString().toLowerCase(),
        Version.parse("0"),
        new Library(base.resolve("lib")),
        List.of(main));
  }

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
    this.library = Util.requireNonNull(library, "library");
    this.realms = List.copyOf(Util.requireNonEmpty(realms, "realms"));
  }

  /** Compute module path for the passed realm. */
  public List<Path> modulePaths(Target target, Path... initialPaths) {
    var paths = new ArrayList<>(List.of(initialPaths));
    if (Files.isDirectory(target.modules)) {
      paths.add(target.modules);
    }
    for (var other : target.realm.realms) {
      var otherTarget = target(other);
      if (Files.isDirectory(otherTarget.modules)) {
        paths.add(otherTarget.modules);
      }
    }
    paths.addAll(Util.findExistingDirectories(library.modulePaths));
    return List.copyOf(paths);
  }

  /** Collection of directories and other realm-specific assets. */
  public class Target {
    /** Associated realm. */
    public final Realm realm;
    /** Base target directory of the realm. */
    public final Path directory;
    /** Directory of modular JAR files. */
    public final Path modules;

    private Target(Realm realm) {
      this.realm = realm;
      this.directory = targetDirectory.resolve("realm").resolve(realm.name);
      this.modules = directory.resolve("modules");
    }

    /** Return base file name for the passed module unit. */
    public String file(ModuleUnit unit) {
      var descriptor = unit.info.descriptor();
      return descriptor.name() + "-" + descriptor.version().orElse(version);
    }

    /** Return file name for the passed module unit. */
    public String file(ModuleUnit unit, String extension) {
      return file(unit) + extension;
    }

    /** Return modular JAR file path for the passed module unit. */
    public Path modularJar(ModuleUnit unit) {
      return modules.resolve(file(unit, ".jar"));
    }

    /** Return sources JAR file path for the passed module unit. */
    public Path sourcesJar(ModuleUnit unit) {
      return directory.resolve(file(unit, "-sources.jar"));
    }
  }

  public Target target(Realm realm) {
    return new Target(realm);
  }

  /** Manage external 3rd-party modules. */
  public static class Library {
    /** List of library paths to external 3rd-party modules. */
    public final List<Path> modulePaths;
    /** Map external 3rd-party module names to their {@code URI}s. */
    public final Function<String, URI> moduleMapper;
    /** Map external 3rd-party module names to their Maven repository. */
    public final Function<String, URI> mavenRepositoryMapper;
    /** Map external 3rd-party module names to their colon-separated Maven Group and Artifact ID. */
    public final UnaryOperator<String> mavenGroupColonArtifactMapper;
    /** Map external 3rd-party module names to their Maven version. */
    public final UnaryOperator<String> mavenVersionMapper;

    public Library(Path lib) {
      this(
          List.of(lib),
          UnmappedModuleException::throwForURI,
          __ -> URI.create("https://repo1.maven.org/maven2"),
          UnmappedModuleException::throwForString,
          UnmappedModuleException::throwForString);
    }

    public Library(
        List<Path> modulePaths,
        Function<String, URI> moduleMapper,
        Function<String, URI> mavenRepositoryMapper,
        UnaryOperator<String> mavenGroupColonArtifactMapper,
        UnaryOperator<String> mavenVersionMapper) {
      this.modulePaths = List.copyOf(Util.requireNonEmpty(modulePaths, "modulePaths"));
      this.moduleMapper = moduleMapper;
      this.mavenRepositoryMapper = mavenRepositoryMapper;
      this.mavenGroupColonArtifactMapper = mavenGroupColonArtifactMapper;
      this.mavenVersionMapper = mavenVersionMapper;
    }
  }

  /** Source-based module reference. */
  public static class ModuleInfo extends ModuleReference {

    /** Module compilation unit parser. */
    public static ModuleInfo of(Path info) {
      if (!Util.isModuleInfo(info)) {
        throw new IllegalArgumentException("Expected module-info.java path, but got: " + info);
      }
      try {
        return new ModuleInfo(Modules.describe(Files.readString(info)), info);
      } catch (IOException e) {
        throw new UncheckedIOException("Reading module declaration failed: " + info, e);
      }
    }


    /** Path to the backing {@code module-info.java} file. */
    public final Path path;
    /** Module source path. */
    public final String moduleSourcePath;

    private ModuleInfo(ModuleDescriptor descriptor, Path path) {
      super(descriptor, path.toUri());
      this.path = path;
      this.moduleSourcePath = Modules.moduleSourcePath(path, descriptor.name());
    }

    @Override
    public ModuleReader open() {
      throw new UnsupportedOperationException("Can't open a module-info.java file for reading");
    }
  }

  /** Java module source unit. */
  public static class ModuleUnit {

    /** Create default unit for the specified path. */
    public static ModuleUnit of(Path path) {
      var reference = ModuleInfo.of(path.resolve("module-info.java"));
      return new ModuleUnit(reference, List.of(path), List.of(), null);
    }

    /** Source-based module reference. */
    public final ModuleInfo info;
    /** Paths to the source directories. */
    public final List<Path> sources;
    /** Paths to the resource directories. */
    public final List<Path> resources;
    /** Path to the associated Maven POM file, may be {@code null}. */
    public final Path mavenPom;

    public ModuleUnit(
            ModuleInfo info, List<Path> sources, List<Path> resources, Path mavenPom) {
      this.info = info;
      this.sources = List.copyOf(sources);
      this.resources = List.copyOf(resources);
      this.mavenPom = mavenPom;
    }

    public String name() {
      return info.descriptor().name();
    }

    public String path() {
      return info.moduleSourcePath;
    }

    public Optional<Path> mavenPom() {
      return Optional.ofNullable(mavenPom);
    }
  }

  /** Multi-release module source unit. */
  public static class MultiReleaseUnit extends ModuleUnit {
    /** Feature release number to source path map. */
    public final Map<Integer, Path> releases;
    /** Copy this module descriptor to the root of the generated modular jar. */
    public final int copyModuleDescriptorToRootRelease;

    public MultiReleaseUnit(
        ModuleInfo info,
        int copyModuleDescriptorToRootRelease,
        Map<Integer, Path> releases,
        List<Path> resources,
        Path mavenPom) {
      super(info, List.copyOf(new TreeMap<>(releases).values()), resources, mavenPom);
      this.copyModuleDescriptorToRootRelease = copyModuleDescriptorToRootRelease;
      this.releases = releases;
    }
  }

  /** Realm-specific tool argument collector. */
  public static class ToolArguments {

    public static final List<String> JAVAC = List.of("-encoding", "UTF-8", "-parameters", "-Xlint");

    public static ToolArguments of() {
      return new ToolArguments(JAVAC, null);
    }

    /** Option values passed to all {@code javac} calls. */
    public final List<String> javac;
    /** Arguments used for uploading modules, may be {@code null}. */
    public final Deployment deployment;

    public ToolArguments(List<String> javac, Deployment deployment) {
      this.javac = List.copyOf(javac);
      this.deployment = deployment;
    }

    public Optional<Deployment> deployment() {
      return Optional.ofNullable(deployment);
    }
  }

  /** Properties used to upload compiled modules. */
  public static class Deployment {
    /** Maven repository id. */
    public final String mavenRepositoryId;
    /** Maven URL as an URI. */
    public final URI mavenUri;

    public Deployment(String mavenRepositoryId, URI mavenUri) {
      this.mavenRepositoryId = mavenRepositoryId;
      this.mavenUri = mavenUri;
    }
  }

  /** Main- and test realms. */
  public static class Realm {

    /** Single module realm factory. */
    public static Realm of(String name, ModuleUnit unit, Realm... realms) {
      var moduleSourcePath = unit.info.moduleSourcePath;
      return new Realm(name, false, 0, moduleSourcePath, ToolArguments.of(), List.of(unit), realms);
    }

    /** Multi-module realm factory. */
    public static Realm of(String name, List<ModuleUnit> units, Realm... realms) {
      var distinctPaths = units.stream().map(ModuleUnit::path).distinct();
      var moduleSourcePath = distinctPaths.collect(Collectors.joining(File.pathSeparator));
      return new Realm(name, false, 0, moduleSourcePath, ToolArguments.of(), units, realms);
    }

    /** Name of the realm. */
    public final String name;
    /** Enable preview features. */
    public final boolean preview;
    /** Java feature release target number. */
    public final int release;
    /** Module source path specifies where to find input source files for multiple modules. */
    public final String moduleSourcePath;
    /** Option values passed to various tools. */
    public final ToolArguments toolArguments;
    /** Map of all declared module source unit. */
    public final List<ModuleUnit> units;
    /** List of required realms. */
    public final List<Realm> realms;

    public Realm(
        String name,
        boolean preview,
        int release,
        String moduleSourcePath,
        ToolArguments toolArguments,
        List<ModuleUnit> units,
        Realm... realms) {
      this.name = name;
      this.preview = preview;
      this.release = release;
      this.moduleSourcePath = moduleSourcePath;
      this.toolArguments = toolArguments;
      this.units = units;
      this.realms = List.of(realms);
    }

    Optional<ModuleUnit> unit(String name) {
      return units.stream().filter(unit -> unit.name().equals(name)).findAny();
    }

    /** Names of all modules declared in this realm. */
    List<String> names() {
      return units.stream().map(ModuleUnit::name).collect(Collectors.toList());
    }

    /** Names of modules declared in this realm of the passed type. */
    List<String> names(Class<? extends ModuleUnit> type) {
      return units.stream()
          .filter(unit -> type.equals(unit.getClass()))
          .map(ModuleUnit::name)
          .collect(Collectors.toList());
    }

    public <T extends ModuleUnit> List<T> units(Class<T> type) {
      return units.stream()
          .filter(unit -> type.equals(unit.getClass()))
          .map(type::cast)
          .collect(Collectors.toList());
    }
  }
}
