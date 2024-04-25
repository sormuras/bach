/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import run.bach.Bach;
import run.bach.internal.ModuleDescriptorSupport;
import run.bach.internal.ModuleDescriptorSupport.ModuleInfoFinder;
import run.bach.internal.ModuleDescriptorSupport.ModuleInfoReference;
import run.bach.internal.ModuleSourcePathSupport;
import run.bach.internal.ModulesSupport;

/** Define Bach's project structure. */
public record Structure(Basics basics, Spaces spaces) {
  /** {@return a list of all modules declared by this project} */
  public List<DeclaredModule> modules() {
    return spaces.list().stream().flatMap(space -> space.modules().list().stream()).toList();
  }

  public String toNameAndVersion() {
    return basics.name() + ' ' + basics.version();
  }

  public Structure with(Space space) {
    return new Structure(basics, spaces.with(space));
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

    public Spaces with(Space space) {
      return new Spaces(append(list, space));
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
      Spaces requires, // used to compute "--[processor-]module-path"
      int release,
      Charset encoding,
      List<Launcher> launchers, // of format: <name> + '=' + <module>[/<main-class>]
      DeclaredModules modules,
      Set<Flag> flags) {

    public enum Flag {
      COMPILE_RUNTIME_IMAGE
    }

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

    public Space(String name, Space... requires) {
      this(
          name,
          new Spaces(requires),
          0,
          StandardCharsets.UTF_8,
          List.of(),
          new DeclaredModules(),
          Set.of());
    }

    public Space withTargetingJavaRelease(int release) {
      return new Space(name, requires, release, encoding, launchers, modules, flags);
    }

    public Space withEncoding(String charset) {
      return withEncoding(Charset.forName(charset));
    }

    public Space withEncoding(Charset encoding) {
      return new Space(name, requires, release, encoding, launchers, modules, flags);
    }

    public Space with(Flag flag) {
      var flags = flags().isEmpty() ? EnumSet.noneOf(Flag.class) : EnumSet.copyOf(flags());
      flags.add(flag);
      return new Space(name, requires, release, encoding, launchers, modules, flags);
    }

    public Space withModule(String content, String info) {
      return with(new DeclaredModule(Path.of(content), Path.of(info)));
    }

    public Space with(DeclaredModule module) {
      var modules = modules().with(module);
      return new Space(name, requires, release, encoding, launchers, modules, flags);
    }

    public Space withLauncher(String launcher) {
      return with(Launcher.of(launcher));
    }

    public Space with(Launcher launcher) {
      var launchers = append(launchers(), launcher);
      return new Space(name, requires, release, encoding, launchers, modules, flags);
    }

    public boolean is(Flag flag) {
      return flags.contains(flag);
    }

    public Optional<Integer> targets() {
      return release == 0 ? Optional.empty() : Optional.of(release);
    }

    public Optional<String> toModulePath(Bach.Folders folders) {
      var externalModules = Stream.of(folders.root("lib"));
      var requiredModules = requires.names().stream().map(name -> folders.out(name, "modules"));
      var elements =
          Stream.concat(requiredModules, externalModules)
              .filter(Files::isDirectory)
              .map(Path::toString)
              .toList();
      if (elements.isEmpty()) return Optional.empty();
      return Optional.of(String.join(File.pathSeparator, elements));
    }

    public ModuleLayer toModuleLayer(Bach.Folders folders, String module) {
      var finder =
          ModuleFinder.compose(
              ModuleFinder.of(folders.out(name(), "modules", module + ".jar")),
              ModuleFinder.of(
                  requires().names().stream()
                      .map(required -> folders.out(required, "modules"))
                      .toArray(Path[]::new)),
              ModuleFinder.of(folders.out(name(), "modules")),
              ModuleFinder.of(folders.root("lib")));
      return ModulesSupport.buildModuleLayer(finder, module);
    }

    public Space toRuntimeSpace() {
      var requires = new Spaces(prepend(this, requires().list()));
      return new Space("runtime", requires, 0, encoding, launchers, modules, flags);
    }
  }

  /** {@code <name> + '=' + <module>[/<main-class>]} */
  public record Launcher(String name, String module, Optional<String> mainClass) {
    public static Launcher of(String string) {
      var equals = string.indexOf('=');
      if (equals == -1 || equals == 0 || equals == string.length() - 1) {
        throw new IllegalArgumentException(string);
      }
      var name = string.substring(0, equals);
      var moduleAndMainClass = string.substring(equals + 1);
      var slash = moduleAndMainClass.indexOf('/');
      if (slash == -1) {
        return new Launcher(name, moduleAndMainClass, Optional.empty());
      }
      if (slash == 0 || slash == moduleAndMainClass.length() - 1) {
        throw new IllegalArgumentException(moduleAndMainClass);
      }
      var module = moduleAndMainClass.substring(0, slash);
      var mainClass = moduleAndMainClass.substring(slash + 1);
      return new Launcher(name, module, Optional.of(mainClass));
    }

    public String toModuleAndMainClass() {
      return module + mainClass.map(c -> '/' + c).orElse("");
    }

    public String toNameAndModuleAndMainClass() {
      return name + '=' + toModuleAndMainClass();
    }
  }

  /** A sequence of declared modules. */
  public record DeclaredModules(List<DeclaredModule> list) implements Iterable<DeclaredModule> {

    public DeclaredModules(DeclaredModule... modules) {
      this(List.of(modules));
    }

    public DeclaredModules with(DeclaredModule module) {
      return new DeclaredModules(append(list, module));
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

    public DeclaredModule withResourcePath(Path path) {
      return new DeclaredModule(content, info, descriptor, base.withResourcePath(path), targeted);
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

    public DeclaredFolders withResourcePath(Path path) {
      if (resources.contains(path)) return this;
      return new DeclaredFolders(sources, append(resources, path));
    }

    public boolean isEmpty() {
      return sources.isEmpty() && resources.isEmpty();
    }
  }

  private static <T> List<T> append(List<T> list, T tail) {
    return Stream.concat(list.stream(), Stream.of(tail)).toList();
  }

  private static <T> List<T> prepend(T head, List<T> list) {
    return Stream.concat(Stream.of(head), list.stream()).toList();
  }
}
