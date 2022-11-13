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

  /** {@return an {@code "unnamed 0-ea"} project with no module source code spaces} */
  public static Project ofDefaults(Bach bach) {
    var cli = bach.cli();
    var name = new Name(cli.projectName("unnamed"));
    var version = new Version(cli.projectVersion("0-ea"), cli.projectVersionTimestampOrNow());
    var spaces = new Spaces();
    var externals = new Externals();
    return new Project(name, version, spaces, externals);
  }

  /** {@return a list of all modules declared by this project} */
  public List<DeclaredModule> modules() {
    return spaces.list().stream().flatMap(space -> space.modules().list().stream()).toList();
  }

  public String toNameAndVersion() {
    return name.value + ' ' + version.value;
  }

  @FunctionalInterface
  public interface Factory {
    Project createProject(Bach bach);
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

    public Version with(ZonedDateTime timestamp) {
      return new Version(value, timestamp);
    }
  }

  public record Spaces(List<Space> list) {
    public Spaces(Space... spaces) {
      this(List.of(spaces));
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
      DeclaredModules modules,
      int release,
      Optional<String> launcher,
      List<String> requires // used to compute "--[processor-]module-path"
      ) {

    public Space {
      if (name.isBlank()) throw new IllegalArgumentException("Space name must not be blank");
      if (modules == null) throw new IllegalArgumentException("Space modules must not be null");
      //noinspection OptionalAssignedToNull
      if (launcher == null) throw new IllegalArgumentException("Space launcher must not be null");
      if (requires == null) throw new IllegalArgumentException("Space requires must not be null");
      var feature = Runtime.version().feature();
      if (release != 0 && (release < 9 || release > feature)) {
        var message = "Java release %d not in range of %d..%d".formatted(release, 9, feature);
        throw new IndexOutOfBoundsException(message);
      }
    }

    public Space(String name, String... requires) {
      this(name, DeclaredModules.of(), 0, Optional.empty(), List.of(requires));
    }

    public Space withModule(Path contentRoot, Path moduleInfo) {
      return withModules(DeclaredModule.of(contentRoot, moduleInfo));
    }

    public Space withModules(DeclaredModule... more) {
      return withModules(modules.with(more));
    }

    public Space withModules(DeclaredModules modules) {
      return new Space(name, modules, release, launcher, requires);
    }

    public Space withTargetsJava(int release) {
      return new Space(name, modules, release, launcher, requires);
    }

    public Space withLauncher(String launcher) {
      return new Space(name, modules, release, Optional.of(launcher), requires);
    }

    public Optional<Integer> targets() {
      return release == 0 ? Optional.empty() : Optional.of(release);
    }

    public Optional<String> toModulePath(Paths paths) {
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
  }

  /** A sequence of declared modules. */
  public record DeclaredModules(List<DeclaredModule> list) implements Iterable<DeclaredModule> {

    public static DeclaredModules of(DeclaredModule... modules) {
      return of(List.of(modules));
    }

    public static DeclaredModules of(List<DeclaredModule> modules) {
      return new DeclaredModules(modules.stream().sorted().toList());
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
      for (var module : list) map.put(module.name(), module.baseSourcePaths());
      return ModuleSourcePathSupport.compute(map, false);
    }

    public DeclaredModules with(DeclaredModule... more) {
      var stream = Stream.concat(list.stream(), Stream.of(more)).sorted();
      return new DeclaredModules(stream.toList());
    }
  }

  public record DeclaredModule(
      Path content, // content root of the entire module
      Path info, // "module-info.java"
      ModuleDescriptor descriptor, // descriptor.name()
      DeclaredFolders base, // base sources and resources
      Map<Integer, DeclaredFolders> targeted)
      implements Comparable<DeclaredModule> {

    public static DeclaredModule of(Path root, Path moduleInfoJavaFileOrItsParentDirectory) {
      var info =
          moduleInfoJavaFileOrItsParentDirectory.endsWith("module-info.java")
              ? moduleInfoJavaFileOrItsParentDirectory
              : moduleInfoJavaFileOrItsParentDirectory.resolve("module-info.java");

      var relativized = root.relativize(info).normalize(); // ensure info is below root
      var descriptor = ModuleDescriptorSupport.parse(info);
      var name = descriptor.name();

      // trivial case: "module-info.java" resides directly in content root directory
      var system = root.getFileSystem();
      if (system.getPathMatcher("glob:module-info.java").matches(relativized)) {
        var base = DeclaredFolders.of(root);
        return new DeclaredModule(root, info, descriptor, base, Map.of());
      }
      // "java" case: "module-info.java" in direct "java" subdirectory with targeted resources
      if (system.getPathMatcher("glob:java/module-info.java").matches(relativized)) {
        var base = DeclaredFolders.of().withSiblings(root);
        var targeted = DeclaredFolders.mapFoldersByJavaFeatureReleaseNumber(root);
        return new DeclaredModule(root, info, descriptor, base, targeted);
      }
      // "<module>" case: "module-info.java" in direct subdirectory with the same name as the module
      if (system.getPathMatcher("glob:" + name + "/module-info.java").matches(relativized)) {
        var content = root.resolve(name);
        var base = DeclaredFolders.of(content).withSiblings(content);
        var targeted = DeclaredFolders.mapFoldersByJavaFeatureReleaseNumber(content);
        return new DeclaredModule(content, info, descriptor, base, targeted);
      }
      // "maven-single" case: "module-info.java" in "src/<space>/java[-module]" directory
      if (system
          .getPathMatcher("glob:src/*/{java,java-module}/module-info.java")
          .matches(relativized)) {
        var java = info.getParent();
        var base = DeclaredFolders.of(java).withSiblings(java.getParent());
        var targeted = DeclaredFolders.mapFoldersByJavaFeatureReleaseNumber(java.getParent());
        return new DeclaredModule(root, info, descriptor, base, targeted);
      }

      // try to find module name in path elements
      var parent = info.getParent();
      while (parent != null
          && !parent.equals(root)
          && !parent.getFileName().toString().equals(name)) {
        parent = parent.getParent();
      }

      if (parent == null || parent.equals(root))
        throw new UnsupportedOperationException("Module name not in path: " + info);
      var content = parent;

      return of(content, info, descriptor);
    }

    static DeclaredModule of(Path content, Path info, ModuleDescriptor descriptor) {
      // "module-info.java" resides in a subdirectory, usually named "java" or "java-module"
      var parent = info.getParent();
      if (parent == null) throw new UnsupportedOperationException("No parent of: " + info);
      var container = parent.getParent();
      if (container == null) throw new UnsupportedOperationException("No container of: " + parent);
      // find base siblings
      var base = DeclaredFolders.of(parent).withSiblings(container);
      // find targeted siblings
      var targeted = DeclaredFolders.mapFoldersByJavaFeatureReleaseNumber(container);
      return new DeclaredModule(content, info, descriptor, base, targeted);
    }

    public String name() {
      return descriptor.name();
    }

    public List<Path> baseSourcePaths() {
      return base.sources();
    }

    @Override
    public int compareTo(DeclaredModule other) {
      return name().compareTo(other.name());
    }
  }

  /** A collection of source and resource directories. */
  public record DeclaredFolders(List<Path> sources, List<Path> resources) {

    static Map<Integer, DeclaredFolders> mapFoldersByJavaFeatureReleaseNumber(Path container) {
      var targeted = new TreeMap<Integer, DeclaredFolders>();
      for (int release = 9; release <= Runtime.version().feature(); release++) {
        var folders = DeclaredFolders.of().withSiblings(container, release);
        if (folders.isEmpty()) continue;
        targeted.put(release, folders);
      }
      return Map.copyOf(targeted);
    }

    public static DeclaredFolders of(Path... sources) {
      return new DeclaredFolders(Stream.of(sources).map(Path::normalize).toList(), List.of());
    }

    public DeclaredFolders withSiblings(Path container) {
      return withSiblings(container, "");
    }

    public DeclaredFolders withSiblings(Path container, int release) {
      return withSiblings(container, "-" + release);
    }

    public DeclaredFolders withSiblings(Path container, String suffix) {
      var sources = container.resolve("java" + suffix);
      var resources = container.resolve("resources" + suffix);
      var folders = this;
      if (Files.isDirectory(sources)) folders = folders.withSourcePath(sources);
      if (Files.isDirectory(resources)) folders = folders.withResourcePath(resources);
      return folders;
    }

    public DeclaredFolders withSourcePath(Path candidate) {
      var path = candidate.normalize();
      if (sources.contains(path)) return this;
      return new DeclaredFolders(
          Stream.concat(sources.stream(), Stream.of(path)).toList(), resources);
    }

    public DeclaredFolders withResourcePath(Path candidate) {
      var path = candidate.normalize();
      if (resources.contains(path)) return this;
      return new DeclaredFolders(
          sources, Stream.concat(resources.stream(), Stream.of(path)).toList());
    }

    public boolean isEmpty() {
      return sources.isEmpty() && resources.isEmpty();
    }
  }

  public record Externals(Set<String> requires) {

    public Externals() {
      this(Set.of());
    }

    public Externals withRequires(String... modules) {
      var requires = Set.copyOf(Stream.concat(requires().stream(), Stream.of(modules)).toList());
      return new Externals(requires);
    }
  }
}
