/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;
import run.bach.Bach;
import run.bach.internal.ModuleDescriptorSupport;
import run.bach.internal.ModuleDescriptorSupport.ModuleInfoFinder;
import run.bach.internal.ModuleDescriptorSupport.ModuleInfoReference;
import run.bach.internal.ModuleSourcePathSupport;

/** Define Bach's project structure. */
public record Structure(Basics basics, Spaces spaces) {
  /** {@return a list of all modules declared by this project} */
  public List<DeclaredModule> modules() {
    return spaces.list().stream().flatMap(space -> space.modules().list().stream()).toList();
  }

  public String toNameAndVersion() {
    return basics.name() + ' ' + basics.version();
  }

  /** Fundamental project-related information. */
  public record Basics(String name, String version, ZonedDateTime timestamp) {
    public Basics(String name, String version) {
      this(name, version, ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }
  }

  public record Spaces(List<Space> list) implements Iterable<Space> {
    public Spaces(Space... spaces) {
      this(List.of(spaces));
    }

    @Override
    public Iterator<Space> iterator() {
      return list.iterator();
    }

    public List<String> names() {
      return list.stream().map(Space::name).toList();
    }

    public Space space(String name) {
      var first = list.stream().filter(space -> space.name().equals(name)).findFirst();
      return first.orElseThrow(() -> new IllegalArgumentException("No such space: " + name));
    }
  }

  public record Space(
      String name,
      List<String> requires, // used to compute "--[processor-]module-path"
      int release,
      List<String> launchers,
      DeclaredModules modules) {

    public Space {
      if (name.isBlank()) throw new IllegalArgumentException("Space name must not be blank");
      if (requires == null) throw new IllegalArgumentException("Space requires must not be null");
      var feature = Runtime.version().feature();
      if (release != 0 && (release < 9 || release > feature)) {
        var message = "Java release %d not in range of %d..%d".formatted(release, 9, feature);
        throw new IndexOutOfBoundsException(message);
      }
      if (launchers == null) throw new IllegalArgumentException("Space launcher must not be null");
      if (modules == null) throw new IllegalArgumentException("Space modules must not be null");
    }

    public Space(String name, int release, String launcher, DeclaredModule... modules) {
      this(name, List.of(), release, List.of(launcher), new DeclaredModules(modules));
    }

    public Optional<Integer> targets() {
      return release == 0 ? Optional.empty() : Optional.of(release);
    }

    public Optional<String> toModulePath(Bach.Folders folders) {
      var externalModules = Stream.of(folders.root("lib"));
      var requiredModules = requires.stream().map(required -> folders.out(required, "modules"));
      var elements =
          Stream.concat(requiredModules, externalModules)
              .filter(Files::isDirectory)
              .map(Path::toString)
              .toList();
      if (elements.isEmpty()) return Optional.empty();
      return Optional.of(String.join(File.pathSeparator, elements));
    }

    public Space toRuntimeSpace() {
      var requires = Stream.concat(Stream.of(name), requires().stream()).toList();
      return new Space("runtime", requires, 0, List.of(), new DeclaredModules());
    }
  }

  /** A sequence of declared modules. */
  public record DeclaredModules(List<DeclaredModule> list) implements Iterable<DeclaredModule> {

    public DeclaredModules(DeclaredModule... modules) {
      this(List.of(modules));
    }

    public Optional<DeclaredModule> find(String name) {
      return list.stream().filter(module -> module.name().equals(name)).findFirst();
    }

    @Override
    public Iterator<DeclaredModule> iterator() {
      return list.iterator();
    }

    public List<String> names() {
      return list.stream().sorted().map(DeclaredModule::name).toList();
    }

    public String names(String delimiter) {
      return String.join(delimiter, names());
    }

    public ModuleFinder toModuleFinder() {
      var moduleInfoReferences =
          list.stream()
              .map(module -> new ModuleInfoReference(module.info(), module.descriptor()))
              .toList();
      return new ModuleInfoFinder(moduleInfoReferences);
    }

    public List<String> toModuleSourcePaths() {
      var map = new TreeMap<String, List<Path>>();
      for (var module : list) map.put(module.name(), module.base().sources());
      return ModuleSourcePathSupport.compute(map, false);
    }
  }

  public record DeclaredModule(
      Path content, // content root of the entire module
      Path info, // "module-info.java"
      ModuleDescriptor descriptor, // descriptor.name()
      DeclaredFolders base, // base sources and resources
      Map<Integer, DeclaredFolders> targeted)
      implements Comparable<DeclaredModule> {
    public DeclaredModule(Path content, Path info) {
      this(
          content,
          info,
          ModuleDescriptorSupport.parse(info),
          new DeclaredFolders(info.getParent()),
          Map.of());
    }

    public String name() {
      return descriptor.name();
    }

    @Override
    public int compareTo(DeclaredModule other) {
      return name().compareTo(other.name());
    }
  }

  /** A collection of source and resource directories. */
  public record DeclaredFolders(List<Path> sources, List<Path> resources) {
    public DeclaredFolders(Path... sources) {
      this(Stream.of(sources).map(Path::normalize).toList(), List.of());
    }

    public boolean isEmpty() {
      return sources.isEmpty() && resources.isEmpty();
    }
  }
}
