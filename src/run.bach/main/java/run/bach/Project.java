package run.bach;

import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import run.bach.internal.ModuleDescriptorSupport;
import run.bach.internal.ModuleInfoFinder;
import run.bach.internal.ModuleInfoReference;
import run.bach.internal.ModuleSourcePathSupport;

/** Modular project model. */
public record Project(Name name, Version version, Spaces spaces, Externals externals) {
  public static final Project UNNAMED = new Project(new Name("unnamed"), new Version("0-ea"));

  public Project(Name name, Version version, Space... spaces) {
    this(name, version, new Spaces(spaces), new Externals());
  }

  /** {@return a list of all modules declared by this project} */
  public List<DeclaredModule> modules() {
    return spaces.list().stream().flatMap(space -> space.modules().list().stream()).toList();
  }

  public String toNameAndVersion() {
    return name.value + ' ' + version.value;
  }

  public record Name(String value) {
    public Name {
      if (value.isBlank()) throw new IllegalArgumentException("Name must not be blank");
    }

    @Override
    public String toString() {
      return value;
    }
  }

  public record Version(String value, ZonedDateTime timestamp) {
    public Version {
      ModuleDescriptor.Version.parse(value);
      timestamp.toInstant();
    }

    public Version(String value) {
      this(value, ZonedDateTime.now());
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
      Optional<String> launcher,
      DeclaredModules modules) {

    public Space {
      if (name.isBlank()) throw new IllegalArgumentException("Space name must not be blank");
      if (requires == null) throw new IllegalArgumentException("Space requires must not be null");
      var feature = Runtime.version().feature();
      if (release != 0 && (release < 9 || release > feature)) {
        var message = "Java release %d not in range of %d..%d".formatted(release, 9, feature);
        throw new IndexOutOfBoundsException(message);
      }
      //noinspection OptionalAssignedToNull
      if (launcher == null) throw new IllegalArgumentException("Space launcher must not be null");
      if (modules == null) throw new IllegalArgumentException("Space modules must not be null");
    }

    public Space(String name, int release, String launcher, DeclaredModule... modules) {
      this(name, List.of(), release, Optional.of(launcher), new DeclaredModules(modules));
    }

    public Optional<Integer> targets() {
      return release == 0 ? Optional.empty() : Optional.of(release);
    }

    public Optional<String> toModulePath(Folders paths) {
      var externalModules = Stream.of(paths.externalModules());
      var requiredModules = requires.stream().map(required -> paths.out(required, "modules"));
      var elements =
          Stream.concat(requiredModules, externalModules)
              .filter(Files::isDirectory)
              .map(Path::toString)
              .toList();
      if (elements.isEmpty()) return Optional.empty();
      return Optional.of(String.join(File.pathSeparator, elements));
    }

    public Space toRuntimeSpace() {
      return new Space("runtime", List.of(name), 0, Optional.empty(), new DeclaredModules());
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

    public DeclaredModule(Path content) {
      this(content, content.resolve("module-info.java"));
    }

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

  public record Externals(Set<String> requires) {
    public Externals {
      if (requires == null) throw new NullPointerException("External requires must not be null");
    }

    public Externals(String... requires) {
      this(Set.of(requires));
    }
  }
}
