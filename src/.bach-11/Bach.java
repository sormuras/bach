/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
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

// default package

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Java Shell Builder.
 *
 * <p>Requires JDK 11 or later.
 */
public class Bach {

  /** Version of the Java Shell Builder. */
  private static final Version VERSION = Version.parse("11.0-ea");

  /** Default printer instance. */
  private static final Consumer<String> PRINTER = System.out::println;

  /** Default verbosity flag, including {@code -Debug} support. */
  private static final boolean VERBOSE =
      Boolean.getBoolean("verbose") // -D verbose=true
          || Boolean.getBoolean("ebug") // -Debug=true
          || "".equals(System.getProperty("ebug")); // -Debug

  /** Bach.java's main program entry-point. */
  public static void main(String... args) {
    try {
      new Bach().new Main(args).run();
    } catch (Throwable throwable) {
      throw new Error("Main program of Bach.java failed", throwable);
    }
  }

  /** Lazily created atomic references to heavyweight objects. */
  private final Atomics atomics;

  /** Line-based message printing consumer. */
  private final Consumer<String> printer;

  /** Verbosity flag. */
  private final boolean verbose;

  /** Initialize this instance with default values. */
  public Bach() {
    this(PRINTER, VERBOSE);
  }

  /** Initialize this instance with the specified arguments. */
  public Bach(Consumer<String> printer, boolean verbose) {
    this.atomics = new Atomics();
    this.printer = printer;
    this.verbose = verbose;
    print(Level.TRACE, "Bach.java initialized");
  }

  /** Atomic accessor. */
  private Atomics atomics() {
    return atomics;
  }

  /** Line printer. */
  private Consumer<String> printer() {
    return printer;
  }

  /** Verbosity flag. */
  public boolean verbose() {
    return verbose;
  }

  /** Build project located in the current working directory. */
  public void build() {
    build(project -> {}).assertSuccessful();
  }

  /** Build project based on the given builder instance. */
  public Build.Summary build(Consumer<Project.Builder> consumer) {
    var base = Path.of("");
    return build(Project.Paths.of(base), consumer);
  }

  /** Build project based on the builder initialized by scanning the given base path. */
  public Build.Summary build(Project.Paths paths, Consumer<Project.Builder> consumer) {
    var builder = new Project.Scanner(paths).scan();
    consumer.accept(builder);
    return build(builder.build());
  }

  /** Build the specified project using the default build factory supplying the root build task. */
  public Build.Summary build(Project project) {
    var factory = new Build.Factory(project, verbose());
    return build(project, factory::build);
  }

  /** Build the specified project using the given root task supplier. */
  public Build.Summary build(Project project, Supplier<Build.Task> task) {
    var start = Instant.now();
    print("Build %s", project.descriptor().toNameAndVersion());
    if (verbose()) project.print(printer());

    var summary = new Build.Summary(project);
    Build.execute(this, task.get(), summary);

    var markdown = summary.write();
    var duration =
        Duration.between(start, Instant.now())
            .toString()
            .substring(2)
            .replaceAll("(\\d[HMS])(?!$)", "$1 ")
            .toLowerCase();

    print("Summary written to %s", markdown.toUri());
    print("Build took %s", duration);
    return summary;
  }

  /** Print a message at information level. */
  public String print(String format, Object... args) {
    return print(Level.INFO, format, args);
  }

  /** Print a message at specified level. */
  public String print(Level level, String format, Object... args) {
    var message = String.format(format, args);
    if (verbose() || level.getSeverity() >= Level.INFO.getSeverity()) printer().accept(message);
    return message;
  }

  /** Project model. */
  public static final class Project implements Util.Printable {

    /** Base path of the project. */
    private final Paths paths;

    /** Project descriptor. */
    private final ModuleDescriptor descriptor;

    /** Project structure. */
    private final Structure structure;

    /** Library of assets. */
    private final Library library;

    /** Initialize this project model. */
    public Project(Paths paths, ModuleDescriptor descriptor, Structure structure, Library library) {
      this.paths = paths;
      this.descriptor = descriptor;
      this.structure = structure;
      this.library = library;
    }

    /** Project paths. */
    public Paths paths() {
      return paths;
    }

    /** Project model descriptor. */
    public ModuleDescriptor descriptor() {
      return descriptor;
    }

    /** Project structure. */
    public Structure structure() {
      return structure;
    }

    /** Library of assets. */
    public Library library() {
      return library;
    }

    /** Return list of modular units. */
    public List<Unit> units() {
      return structure().units();
    }

    /** Return list of modular realms. */
    public List<Realm> realms() {
      return structure().realms();
    }

    /** Return the project main module. */
    public Optional<String> mainModule() {
      return descriptor.mainClass();
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", "Project [", "]")
          .add("paths=" + paths())
          .add("descriptor=" + descriptor())
          .add("mainModule=" + mainModule())
          .add("loader=" + library())
          .toString();
    }

    /** Compose JAR file name by using unit and project properties. */
    public String toJarName(Unit unit, String classifier) {
      var unitVersion = unit.descriptor().version();
      var projectVersion = descriptor().version();
      var version = unitVersion.isPresent() ? unitVersion : projectVersion;
      var versionSuffix = version.map(v -> "-" + v).orElse("");
      var classifierSuffix = classifier.isEmpty() ? "" : "-" + classifier;
      return unit.name() + versionSuffix + classifierSuffix + ".jar";
    }

    /** Join all unit names of the given realm to a comma-separated string. */
    public String toModule(Realm realm) {
      var names = realm.units().values().stream().map(Unit::name);
      return names.collect(Collectors.joining(","));
    }

    /** Compose path to the Java module specified by its realm and modular unit. */
    public Path toModularJar(Realm realm, Unit unit) {
      return paths().modules(realm).resolve(toJarName(unit, ""));
    }

    /** Generate {@code --module-path} string for the specified realm. */
    public String toModulePath(Realm realm) {
      return toModulePaths(realm).stream()
          .map(Path::toString)
          .collect(Collectors.joining(File.pathSeparator));
    }

    /** Generate list of path for the specified realm. */
    public List<Path> toModulePaths(Realm realm) {
      var paths = new ArrayList<Path>();
      realm.dependencies().stream().map(paths()::modules).forEach(paths::add);
      paths.add(paths().lib());
      return paths;
    }

    /** Source directory tree layout. */
    public enum Layout {
      /**
       * Unnamed or flat realm source directory tree layout.
       *
       * <ul>
       *   <li>{@code ${SRC}/${MODULE}/module-info.java}
       * </ul>
       *
       * Module source path examples:
       *
       * <ul>
       *   <li>{@code --module-source-path .}
       *   <li>{@code --module-source-path src}
       *   <li>{@code --module-source-path src/modules}
       *   <li>{@code --module-source-path src/group-1:src/group-2}
       * </ul>
       *
       * @see <a href="https://openjdk.java.net/projects/jigsaw/quick-start">Project Jigsaw: Module
       *     System Quick-Start Guide</a>
       */
      FLAT,

      /**
       * Source tree layout with main and test realm and nested categories.
       *
       * <ul>
       *   <li>{@code ${SRC}/${MODULE}/${OFFSET}/main/java/module-info.java}
       *   <li>{@code ${SRC}/${MODULE}/${OFFSET}/test/java/module-info.java}
       *   <li>{@code ${SRC}/${MODULE}/${OFFSET}/test/module/module-info.java}
       * </ul>
       *
       * Module source path examples:
       *
       * <ul>
       *   <li>{@code --module-source-path src/ * /main/java}
       *   <li>{@code --module-source-path src/ * /test/java:src/ * /test/module}
       * </ul>
       */
      MAIN_TEST {
        @Override
        public Optional<String> realmOf(Unit unit) {
          var deque = deque(unit);
          var category = deque.pop();
          if (!(category.equals("java")
              || category.matches("java-\\d+")
              || category.equals("module"))) return Optional.empty();
          var realm = deque.pop();
          if (!(realm.equals("main") || realm.equals("test"))) return Optional.empty();
          if (Collections.frequency(deque(unit), unit.name()) != 1) return Optional.empty();
          return Optional.of(realm);
        }

        @Override
        public List<Realm> realmsOf(List<Unit> units) {
          var map = new HashMap<String, List<Unit>>();
          for (var unit : units) {
            var realm = realmOf(unit).orElse(unit.path().toString());
            map.computeIfAbsent(realm, key -> new ArrayList<>()).add(unit);
          }
          var realms = new ArrayList<Realm>();
          var main = map.remove("main");
          if (main != null) {
            realms.add(
                new Realm(
                    "main",
                    EnumSet.of(Realm.Modifier.CREATE_JAVADOC),
                    0,
                    moduleSourcePath(main),
                    Unit.toMap(units.stream().filter(u -> realmOf(u).orElseThrow().equals("main"))),
                    List.of()));
          }
          var test = map.remove("test");
          if (test != null) {
            realms.add(
                new Realm(
                    "test",
                    // EnumSet.of(Realm.Modifier.LAUNCH_TESTS, Realm.Modifier.ENABLE_PREVIEW),
                    EnumSet.of(Realm.Modifier.LAUNCH_TESTS),
                    0,
                    moduleSourcePath(test),
                    Unit.toMap(units.stream().filter(u -> realmOf(u).orElseThrow().equals("test"))),
                    realms));
          }
          if (map.isEmpty()) return realms;
          throw new IllegalStateException("Expected main and test realms only: " + map.keySet());
        }
      };

      /** Extract the name of the realm from the given modular unit. */
      public Optional<String> realmOf(Unit unit) {
        if (Collections.frequency(deque(unit), unit.name()) != 1) return Optional.empty();
        return Optional.of("");
      }

      /** Create realms based on the given units. */
      public List<Realm> realmsOf(List<Unit> units) {
        if (units.isEmpty()) return List.of();
        var name = "";
        var modifiers = EnumSet.of(Realm.Modifier.CREATE_JAVADOC);
        var moduleSourcePath = moduleSourcePath(units);
        var modules = Unit.toMap(units.stream());
        return List.of(new Realm(name, modifiers, 0, moduleSourcePath, modules, List.of()));
      }

      /** Convert path element names of the given unit into a reversed deque. */
      public static Deque<String> deque(Unit unit) {
        var path = unit.path();
        var deque = new ArrayDeque<String>();
        path.forEach(name -> deque.addFirst(name.toString()));
        var info = deque.pop();
        if (!info.equals("module-info.java"))
          throw new IllegalArgumentException("No module-info.java?! " + path);
        return deque;
      }

      /** Compute distinct module source path from the given list of units. */
      public static String moduleSourcePath(List<Unit> units) {
        return units.stream()
            .map(Unit::moduleSourcePath)
            .distinct()
            .collect(Collectors.joining(File.pathSeparator));
      }

      /** Scan the given units for a well-known modular source layout. */
      public static Optional<Layout> find(List<Unit> units) {
        for (var layout : List.of(Layout.MAIN_TEST, Layout.FLAT))
          if (units.stream().allMatch(unit -> layout.realmOf(unit).isPresent()))
            return Optional.of(layout);
        return Optional.empty();
      }
    }

    /** Common project-related paths. */
    public static final class Paths implements Util.Printable {

      public static final Path CLASSES = Path.of("classes");
      public static final Path MODULES = Path.of("modules");
      public static final Path SOURCES = Path.of("sources");
      public static final Path DOCUMENTATION = Path.of("documentation");
      public static final Path JAVADOC = DOCUMENTATION.resolve("javadoc");

      public static Paths of(Path base) {
        return new Paths(base, base.resolve(".bach"), base.resolve("lib"));
      }

      private final Path base;
      private final Path out;
      private final Path lib;

      public Paths(Path base, Path out, Path lib) {
        this.base = base;
        this.out = out;
        this.lib = lib;
      }

      public Path base() {
        return base;
      }

      public Path out() {
        return out;
      }

      public Path out(String first, String... more) {
        var path = Path.of(first, more);
        return out.resolve(path);
      }

      public Path classes(Realm realm) {
        return out.resolve(CLASSES).resolve(realm.name());
      }

      public Path javadoc() {
        return out.resolve(JAVADOC);
      }

      public Path modules(Realm realm) {
        return out.resolve(MODULES).resolve(realm.name());
      }

      public Path sources(Realm realm) {
        return out.resolve(SOURCES).resolve(realm.name());
      }

      public Path lib() {
        return lib;
      }

      @Override
      public String toString() {
        return new StringJoiner(", ", Paths.class.getSimpleName() + "[", "]")
            .add("base='" + base() + "'")
            .add("out='" + out() + "'")
            .add("lib='" + lib() + "'")
            .toString();
      }
    }

    /** Single source path with optional release directive. */
    public static final class Source {

      /** Source-specific modifier enumeration. */
      public enum Modifier {
        /** Store binary assets in {@code META-INF/versions/${release}/} directory of the jar. */
        VERSIONED
      }

      /** Create default non-targeted source for the specified path and optional modifiers. */
      public static Source of(Path path, Modifier... modifiers) {
        return new Source(path, 0, Set.of(modifiers));
      }

      private final Path path;
      private final int release;
      private final Set<Modifier> modifiers;

      public Source(Path path, int release, Set<Modifier> modifiers) {
        this.path = path;
        this.release = release;
        this.modifiers = modifiers.isEmpty() ? Set.of() : EnumSet.copyOf(modifiers);
      }

      /** Source path. */
      public Path path() {
        return path;
      }

      /** Java feature release target number, with zero indicating the current runtime release. */
      public int release() {
        return release;
      }

      /** This source modifiers. */
      public Set<Modifier> modifiers() {
        return modifiers;
      }

      public boolean isTargeted() {
        return release != 0;
      }

      /** Optional Java feature release target number. */
      public OptionalInt target() {
        return isTargeted() ? OptionalInt.of(release) : OptionalInt.empty();
      }

      public boolean isVersioned() {
        return modifiers.contains(Modifier.VERSIONED);
      }

      @Override
      public String toString() {
        return new StringJoiner(", ", "Source { ", " }")
            .add("path=" + path())
            .add("release=" + release())
            .add("target=" + target())
            .add("modifiers=" + modifiers())
            .toString();
      }
    }

    /** A modular project structure. */
    public static final class Structure implements Util.Printable {

      /** List of all declared modular units. */
      private final List<Unit> units;

      /** List of all realms. */
      private final List<Realm> realms;

      /** Project module survey */
      private final ModuleSurvey survey;

      public Structure(List<Unit> units, List<Realm> realms, ModuleSurvey survey) {
        this.units = List.copyOf(units);
        this.realms = List.copyOf(realms);
        this.survey = survey;
      }

      public List<Unit> units() {
        return units;
      }

      public List<Realm> realms() {
        return realms;
      }

      public ModuleSurvey survey() {
        return survey;
      }

      @Override
      public String toString() {
        return new StringJoiner(", ", "Structure { ", " }")
            .add("units=" + units())
            .add("realms=" + realms())
            .add("survey=" + survey())
            .toString();
      }
    }

    /** A module source description unit. */
    public static final class Unit implements Util.Printable {

      public static Map<String, Unit> toMap(Stream<Unit> units) {
        return units.collect(Collectors.toMap(Unit::name, Function.identity()));
      }

      private static List<Path> resources(Path path) {
        var resources = path.resolveSibling("resources");
        return Files.isDirectory(resources) ? List.of(resources) : List.of();
      }

      private final Path path;
      private final ModuleDescriptor descriptor;
      private final String moduleSourcePath;
      private final List<Source> sources;
      private final List<Path> resources;

      private Unit(Path info) {
        this(info, Util.Modules.describe(info));
      }

      private Unit(Path info, ModuleDescriptor descriptor) {
        this(
            info,
            descriptor,
            Util.Modules.moduleSourcePath(info, descriptor.name()),
            List.of(Source.of(info.getParent())),
            resources(info.getParent()));
      }

      public Unit(
          Path path,
          ModuleDescriptor descriptor,
          String moduleSourcePath,
          List<Source> sources,
          List<Path> resources) {
        this.path = path;
        this.descriptor = descriptor;
        this.moduleSourcePath = moduleSourcePath;
        this.sources = sources;
        this.resources = resources;
      }

      public Path path() {
        return path;
      }

      public ModuleDescriptor descriptor() {
        return descriptor;
      }

      public String moduleSourcePath() {
        return moduleSourcePath;
      }

      public List<Source> sources() {
        return sources;
      }

      public List<Path> resources() {
        return resources;
      }

      @Override
      public String toString() {
        @SuppressWarnings("StringBufferReplaceableByString")
        var sb = new StringBuilder("Unit{");
        sb.append("path=").append(path());
        sb.append(", descriptor=").append(descriptor());
        sb.append(", moduleSourcePath=").append(moduleSourcePath());
        sb.append(", sources=").append(sources());
        sb.append(", resources=").append(resources());
        sb.append(", isMultiRelease=").append(isMultiRelease());
        sb.append(", isMainClassPresent=").append(isMainClassPresent());
        sb.append('}');
        return sb.toString();
      }

      public String name() {
        return descriptor.name();
      }

      @Override
      public String printCaption() {
        return "Unit '" + name() + "'";
      }

      public <T> List<T> sources(Function<Source, T> mapper) {
        return sources.stream().map(mapper).collect(Collectors.toList());
      }

      public boolean isMultiRelease() {
        return !sources.isEmpty() && sources.stream().allMatch(Source::isTargeted);
      }

      public boolean isMainClassPresent() {
        return descriptor.mainClass().isPresent();
      }
    }

    /** A realm of modular sources. */
    public static final class Realm implements Util.Printable {

      /** Realm-related flags controlling the build process. */
      public enum Modifier {
        ENABLE_PREVIEW,
        CREATE_JAVADOC,
        LAUNCH_TESTS
      }

      private final String name;
      private final Set<Modifier> modifiers;
      private final int release;
      private final String moduleSourcePath;
      private final Map<String, Unit> units;
      private final List<Realm> dependencies;

      public Realm(
          String name,
          Set<Modifier> modifiers,
          int release,
          String moduleSourcePath,
          Map<String, Unit> units,
          List<Realm> dependencies) {
        this.name = name;
        this.modifiers = modifiers.isEmpty() ? Set.of() : EnumSet.copyOf(modifiers);
        this.release = release;
        this.moduleSourcePath = moduleSourcePath;
        this.units = Map.copyOf(units);
        this.dependencies = List.copyOf(dependencies);
      }

      public String name() {
        return name;
      }

      public Set<Modifier> modifiers() {
        return modifiers;
      }

      public OptionalInt release() {
        return release == 0 ? OptionalInt.empty() : OptionalInt.of(release);
      }

      public String moduleSourcePath() {
        return moduleSourcePath;
      }

      public Map<String, Unit> units() {
        return units;
      }

      public List<Realm> dependencies() {
        return dependencies;
      }

      @Override
      public String toString() {
        return new StringJoiner(", ", "Realm { ", " }")
            .add("name='" + name + "'")
            .add("modifiers=" + modifiers)
            .add("release=" + release)
            .add("units=" + units)
            .add("dependencies=" + dependencies)
            .toString();
      }

      public boolean lacks(Modifier modifier) {
        return !modifiers.contains(modifier);
      }

      public boolean test(Modifier modifier) {
        return modifiers.contains(modifier);
      }

      /** Generate {@code --patch-module} strings for this realm. */
      public List<String> patches(BiFunction<Realm, Unit, List<Path>> patcher) {
        if (dependencies().isEmpty()) return List.of();
        var patches = new ArrayList<String>();
        for (var unit : units().values()) {
          var module = unit.name();
          for (var dependency : dependencies()) {
            var other = dependency.units().get(module);
            if (other == null) continue;
            var paths = Util.Strings.join(patcher.apply(dependency, other));
            patches.add(module + '=' + paths);
          }
        }
        return patches.isEmpty() ? List.of() : List.copyOf(patches);
      }

      @Override
      public String printCaption() {
        return "Realm '" + name() + "'";
      }

      @Override
      public boolean printTest(String name, Object value) {
        return !name.equals("units");
      }
    }

    /** Library of assets. */
    public static final class Library implements Util.Printable {

      public enum Modifier {
        RESOLVE_RECURSIVELY,
        ADD_MISSING_JUNIT_TEST_ENGINES,
        ADD_MISSING_JUNIT_PLATFORM_CONSOLE
      }

      private final Set<Modifier> modifiers;
      private final ModuleMapper mapper;

      public Library(Set<Modifier> modifiers, ModuleMapper mapper) {
        this.modifiers = modifiers.isEmpty() ? Set.of() : EnumSet.copyOf(modifiers);
        this.mapper = mapper;
      }

      public ModuleMapper mapper() {
        return mapper;
      }

      @Override
      public String toString() {
        return new StringJoiner(", ", Library.class.getSimpleName() + "[", "]")
            .add("mapper=" + mapper())
            .toString();
      }

      /** Check the specified flag. */
      public boolean test(Modifier modifier) {
        return modifiers.contains(modifier);
      }

      /** Look up the mapped URI for the given pair of a module name and its version. */
      public URI uri(String module, Version version) {
        var mapping = mapper().apply(module, version);
        if (mapping == null) throw new Util.Modules.UnmappedModuleException(module);
        return mapping.uri();
      }
    }

    /** Mapping module names to their related coordinates. */
    public interface ModuleMapper extends BiFunction<String, Version, ModuleMapper.Mapping> {

      /** Create default Maven Central module mapper. */
      static ModuleMapper ofMavenCentral(Map<String, String> mavens) {
        return new MavenCentral.DefaultMapper(MavenCentral.map(mavens));
      }

      /** Return mapping instance for the pair of module name and its version - {@code null}able. */
      @Override
      Mapping apply(String module, Version version);

      /** Module and version to URI and more other-system-property mapping. */
      final class Mapping implements Util.Printable {

        public static URI uri(
            String repository,
            String group,
            String artifact,
            String version,
            String classifier,
            String type) {
          var versionAndClassifier = classifier.isEmpty() ? version : version + '-' + classifier;
          var file = artifact + '-' + versionAndClassifier + '.' + type;
          var ref = String.join("/", repository, group.replace('.', '/'), artifact, version, file);
          return URI.create(ref);
        }

        private final String module;
        private final Version version;
        private final URI uri;

        public Mapping(String module, Version version, URI uri) {
          this.module = module;
          this.version = version;
          this.uri = uri;
        }

        public String module() {
          return module;
        }

        public Version version() {
          return version;
        }

        public URI uri() {
          return uri;
        }

        @Override
        public String toString() {
          return new StringJoiner(", ", "ModuleLink { ", " }")
              .add("module='" + module() + "'")
              .add("version=" + version())
              .add("uri=" + uri())
              .toString();
        }
      }

      /** Mapping Maven Central coordinates. */
      interface MavenCentral {

        String REPOSITORY = "https://repo.maven.apache.org/maven2";

        /** Transform Maven Central based coordinates to a mapping instance. */
        static Mapping map(
            String module, Version version, String group, String artifact, String classifier) {
          var uri = Mapping.uri(REPOSITORY, group, artifact, version.toString(), classifier, "jar");
          return new Mapping(module, version, uri);
        }

        /** Transform Maven Central based coordinates to a map of mappings. */
        static Map<String, Mapping> map(Map<String, String> coordinates) {
          var mappings = new TreeMap<String, Mapping>();
          for (var entry : coordinates.entrySet()) {
            var module = entry.getKey();
            var coordinate = entry.getValue().split(":");
            var group = coordinate[0];
            var artifact = coordinate[1];
            var version = Version.parse(coordinate[2]);
            var classifier = coordinate.length < 4 ? "" : coordinate[3];
            mappings.put(module, map(module, version, group, artifact, classifier));
          }
          return mappings;
        }

        /** Mapping well-known module names to their related Maven Central coordinates. */
        @SuppressWarnings({"unused", "SwitchStatementWithTooFewBranches"})
        class DefaultMapper implements ModuleMapper {

          private final Map<String, Mapping> constants;

          public DefaultMapper(Map<String, Mapping> constants) {
            this.constants = constants;
          }

          @Override
          public Mapping apply(String module, Version version) {
            if (constants.containsKey(module)) return constants.get(module);
            var group = mapGroup(module, version);
            var artifact = mapArtifact(module, version, group);
            var version2 = mapVersion(module, version, group, artifact);
            var classifier = mapClassifier(module, version, group, artifact);
            return MavenCentral.map(module, version2, group, artifact, classifier);
          }

          public String mapGroup(String module, Version version) {
            if (module.startsWith("org.junit.platform")) return "org.junit.platform";
            if (module.startsWith("org.junit.jupiter")) return "org.junit.jupiter";
            if (module.startsWith("org.junit.vintage")) return "org.junit.vintage";
            switch (module) {
              case "junit":
                return "junit";
            }
            throw new Util.Modules.UnmappedModuleException(module);
          }

          public String mapArtifact(String module, Version version, String group) {
            // Group ID pattern: "org.junit.[jupiter|platform|vintage|.*]"
            if (group.startsWith("org.junit.")) return module.substring(4).replace('.', '-');
            switch (module) {
              case "junit":
                return "junit";
            }
            throw new Util.Modules.UnmappedModuleException(module);
          }

          public Version mapVersion(String module, Version version, String group, String artifact) {
            if (version != null) return version;
            switch (group) {
              case "junit":
                return Version.parse("4.13");
              case "org.junit.jupiter":
              case "org.junit.vintage":
                return Version.parse("5.6.0");
              case "org.junit.platform":
                return Version.parse("1.6.0");
            }
            throw new Util.Modules.UnmappedModuleException(module);
          }

          public String mapClassifier(
              String module, Version version, String group, String artifact) {
            return "";
          }

          @Override
          public String toString() {
            return getClass().getSimpleName();
          }
        }

        /** https://github.com/sormuras/modules */
        class SormurasModulesMapper implements ModuleMapper {

          private static final String ROOT = "https://github.com/sormuras/modules";

          private final Map<String, String> mavens;
          private final Map<String, String> versions;

          public SormurasModulesMapper(Util.Uris uris) throws Exception {
            this.mavens = load(uris, "module-maven.properties");
            this.versions = load(uris, "module-version.properties");
          }

          private Map<String, String> load(Util.Uris uris, String properties) throws Exception {
            var user = Path.of(System.getProperty("user.home"));
            var cache = Files.createDirectories(user.resolve(".bach/modules"));
            var source = URI.create(String.join("/", ROOT, "raw/master", properties));
            var target = cache.resolve(properties);
            var path = uris.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
            return Util.Strings.map(Util.Strings.load(new Properties(), path));
          }

          @Override
          public Mapping apply(String module, Version version) {
            var maven = mavens.get(module);
            if (maven == null) throw new Util.Modules.UnmappedModuleException(module);
            var indexOfColon = maven.indexOf(':');
            if (indexOfColon < 0) throw new AssertionError("Expected group:artifact: " + maven);
            var group = maven.substring(0, indexOfColon);
            var artifact = maven.substring(indexOfColon + 1);
            if (version == null) {
              var latest = versions.get(module);
              if (latest == null) throw new Util.Modules.UnmappedModuleException(module);
              version = Version.parse(latest);
            }
            return ModuleMapper.MavenCentral.map(module, version, group, artifact, "");
          }
        }
      }
    }

    /** Declared and required modules and optional versions holder. */
    public static final class ModuleSurvey implements Util.Printable {

      /** Create modular survey by scanning the locatable modules of the given module finder. */
      public static ModuleSurvey of(ModuleFinder finder) {
        return of(finder.findAll().stream().map(ModuleReference::descriptor));
      }

      /** Create modular survey by scanning the given stream of module descriptors. */
      public static ModuleSurvey of(Stream<ModuleDescriptor> descriptors) {
        var declaredModules = new TreeSet<String>();
        var requiredModules = new TreeMap<String, Version>();
        descriptors
            .peek(descriptor -> declaredModules.add(descriptor.name()))
            .map(ModuleDescriptor::requires)
            .flatMap(Set::stream)
            .filter(requires -> !requires.modifiers().contains(Requires.Modifier.STATIC))
            .filter(requires -> !requires.modifiers().contains(Requires.Modifier.MANDATED))
            .forEach(
                requires -> {
                  var module = requires.name();
                  var version = requires.compiledVersion().orElse(null);
                  var previous = requiredModules.putIfAbsent(module, version);
                  if (previous == null || version == null || previous.equals(version)) return;
                  throw new IllegalArgumentException(previous + " != " + version);
                });
        return new ModuleSurvey(declaredModules, requiredModules);
      }

      final Set<String> declaredModules;
      final Map<String, Version> requiredModules;

      ModuleSurvey(Set<String> declaredModules, Map<String, Version> requiredModules) {
        this.declaredModules = declaredModules;
        this.requiredModules = requiredModules;
      }

      public Set<String> declaredModules() {
        return declaredModules;
      }

      public Map<String, Version> requiredModules() {
        return requiredModules;
      }

      public Set<String> requiredModuleNames() {
        return requiredModules.keySet();
      }

      public Optional<Version> requiredVersion(String requiredModule) {
        var unmapped = !requiredModules.containsKey(requiredModule);
        if (unmapped) throw new Util.Modules.UnmappedModuleException(requiredModule);
        return Optional.ofNullable(requiredModules.get(requiredModule));
      }
    }

    /** Project model builder. */
    public static class Builder {

      /** Paths of the project. */
      private Paths paths;

      /** Project model descriptor builder. */
      private final ModuleDescriptor.Builder descriptor;

      /** List of modular units. */
      private List<Unit> units;

      /** List of modular realms. */
      private List<Realm> realms;

      /** Project module survey. */
      private ModuleSurvey survey;

      /** Mapper of modules. */
      private ModuleMapper mapper;

      /** Module to Maven coordinates constants. */
      private final Map<String, String> mavens;

      /** Initialize this project model builder with the given name. */
      Builder(String name) {
        this.paths = Paths.of(Path.of(""));
        var synthetic = Set.of(ModuleDescriptor.Modifier.SYNTHETIC);
        this.descriptor = ModuleDescriptor.newModule(name, synthetic);
        this.units = List.of();
        this.realms = List.of();
        this.survey = new ModuleSurvey(Set.of(), Map.of());
        this.mapper = null;
        this.mavens = new TreeMap<>();
      }

      /** Create new project model instance based on this builder's components. */
      public Project build() {
        var temporary = descriptor.build();
        if (temporary.mainClass().isEmpty()) {
          Convention.mainModule(units.stream()).ifPresent(descriptor::mainClass);
        }
        var structure = new Structure(units, realms, survey);
        var mapper = this.mapper != null ? this.mapper : ModuleMapper.ofMavenCentral(mavens);
        var library = new Library(EnumSet.allOf(Library.Modifier.class), mapper);
        return new Project(paths, descriptor.build(), structure, library);
      }

      /** Set base directory of the project. */
      public Builder paths(Path base) {
        return paths(Paths.of(base));
      }

      /** Set paths of the project. */
      public Builder paths(Paths paths) {
        this.paths = paths;
        return this;
      }

      /** Set module mapper. */
      public Builder mapper(ModuleMapper mapper) {
        this.mapper = mapper;
        return this;
      }

      /** Put a module name to Maven Central coordinates mapping. */
      public Builder map(String module, String maven) {
        mavens.put(module, maven);
        return this;
      }

      /** Declare a dependence on the specified module and version. */
      public Builder requires(String module, String version) {
        var synthetic = Set.of(Requires.Modifier.SYNTHETIC);
        descriptor.requires(synthetic, module, Version.parse(version));
        return this;
      }

      /** Set list of modular units. */
      public Builder units(List<Unit> units) {
        this.units = units;
        return this;
      }

      /** Set list of modular realms. */
      public Builder realms(List<Realm> realms) {
        this.realms = realms;
        return this;
      }

      /** Set project module survey. */
      public Builder survey(ModuleSurvey survey) {
        this.survey = survey;
        return this;
      }

      /** Set the version of the project. */
      public Builder version(String version) {
        descriptor.version(version);
        return this;
      }
    }

    /** Directory-based project model builder factory. */
    public static class Scanner {
      private final Paths paths;

      public Scanner(Paths paths) {
        this.paths = paths;
      }

      public Path base() {
        return paths.base();
      }

      /** Scan the base directory for project components. */
      public Project.Builder scan() {
        var units = scanUnits();
        var layout = Layout.find(units).orElse(Layout.FLAT);
        var survey = ModuleSurvey.of(units.stream().map(Unit::descriptor));
        return new Project.Builder(scanName().orElse("nameless"))
            .paths(paths)
            .units(units)
            .realms(layout.realmsOf(units))
            .survey(survey);
      }

      /** Return name of the project. */
      public Optional<String> scanName() {
        return Optional.of(base().toAbsolutePath().getFileName())
            .map(Object::toString)
            .map(name -> name.replace(' ', '.'))
            .map(name -> name.replace('-', '.'))
            .map(name -> name.replace('_', '.'));
      }

      /** Scan for modular source units. */
      public List<Unit> scanUnits() {
        var base = paths.base();
        var src = base.resolve("src"); // More subdirectory candidates? E.g. "modules", "sources"?
        var root = Files.isDirectory(src) ? src : base;
        try (var stream = Files.find(root, 5, (path, __) -> path.endsWith("module-info.java"))) {
          return stream.map(Unit::new).collect(Collectors.toList());
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    }
  }

  /** Common project conventions. */
  interface Convention {

    /** Return name of main class of the specified module. */
    static Optional<String> mainClass(Path info, String module) {
      var main = Path.of(module.replace('.', '/'), "Main.java");
      var exists = Files.isRegularFile(info.resolveSibling(main));
      return exists ? Optional.of(module + '.' + "Main") : Optional.empty();
    }

    /** Return name of the main module by finding a single main class containing unit. */
    static Optional<String> mainModule(Stream<Project.Unit> units) {
      var mains = units.filter(Project.Unit::isMainClassPresent).collect(Collectors.toList());
      return mains.size() == 1 ? Optional.of(mains.get(0).name()) : Optional.empty();
    }

    /** Extend the passed map of modules with missing JUnit test engine implementations. */
    static void addMissingJUnitTestEngines(Map<String, Version> modules) {
      var names = modules.keySet();
      if (names.contains("org.junit.jupiter") || names.contains("org.junit.jupiter.api"))
        modules.putIfAbsent("org.junit.jupiter.engine", null);
      if (names.contains("junit")) modules.putIfAbsent("org.junit.vintage.engine", null);
    }

    /** Extend the passed map of modules with the JUnit Platform Console module. */
    static void addJUnitPlatformConsole(Map<String, Version> modules) {
      var names = modules.keySet();
      if (names.contains("org.junit.platform.console")) return;
      var triggers =
          Set.of(
              "org.junit.jupiter.engine", "org.junit.vintage.engine", "org.junit.platform.engine");
      names.stream()
          .filter(triggers::contains)
          .findAny()
          .ifPresent(__ -> modules.put("org.junit.platform.console", null));
    }
  }

  /** Namespace for build-related types. */
  public interface Build {

    /** Implement this marker interface indicating a {@link System#gc()} call is required. */
    interface GarbageCollect {}

    /** Run the given task and its attached child tasks. */
    static void execute(Bach bach, Task task, Summary summary) {
      var verbose = bach.verbose();
      var printer = bach.printer();

      var markdown = task.toMarkdown();
      if (verbose) printer.accept(markdown);

      summary.executionBegin(task);
      var result = task.execute(new Task.Execution(bach));
      if (verbose) {
        result.out.lines().forEach(printer);
        result.err.lines().forEach(printer);
      }
      if (result.code != 0) {
        result.err.lines().forEach(printer);
        summary.executionEnd(task, result);
        var message = markdown + ": non-zero result code: " + result.code;
        throw new TaskExecutionException(message);
      }

      var children = task.children;
      if (!children.isEmpty()) {
        try {
          var tasks = task.parallel ? children.parallelStream() : children.stream();
          tasks.forEach(child -> execute(bach, child, summary));
        } catch (RuntimeException e) {
          summary.error.addSuppressed(e);
        }
      }

      summary.executionEnd(task, result);
    }

    /** Group the given tasks for parallel execution. */
    static Task parallel(String caption, Task... tasks) {
      return new Build.Task(caption, true, List.of(tasks));
    }

    /** Group the given tasks for sequential execution. */
    static Task sequence(String caption, Task... tasks) {
      return new Build.Task(caption, false, List.of(tasks));
    }

    /** Create new tool-running task for the given tool name. */
    static Task tool(String name, String... args) {
      var caption = String.format("Run `%s` with %d argument(s)", name, args.length);
      var provider = ToolProvider.findFirst(name);
      var tool = provider.orElseThrow(() -> new ToolNotFoundException(name));
      return new Task.RunTool(caption, tool, args);
    }

    /** Build task factory. */
    class Factory {

      private final Project project;
      private final boolean verbose;

      public Factory(Project project, boolean verbose) {
        this.project = project;
        this.verbose = verbose;
      }

      public Task build() {
        return sequence(
            "Build project " + project.descriptor().name(),
            new Task.CreateDirectories(project.paths().out()),
            parallel(
                "Print version of various foundation tools",
                tool("javac", "--version"),
                tool("javadoc", "--version"),
                tool("jar", "--version")),
            resolveMissingModules(),
            parallel(
                "Compile and generate API documentation",
                compileAllRealms(),
                compileApiDocumentation()),
            launchAllTests());
      }

      public Task resolveMissingModules() {
        return new Resolver();
      }

      public Task compileAllRealms() {
        var realms = project.structure().realms();
        if (realms.isEmpty()) return sequence("Cannot compile modules: 0 realms declared");
        var tasks = realms.stream().map(this::compileRealm);
        return sequence("Compile all realms", tasks.toArray(Task[]::new));
      }

      public Task compileRealm(Project.Realm realm) {
        if (realm.units.isEmpty()) return sequence("No units in " + realm.name + " realm?!");
        var module = project.toModule(realm);
        var classes = project.paths().classes(realm);
        var enablePreview = realm.test(Project.Realm.Modifier.ENABLE_PREVIEW);
        var release = enablePreview ? OptionalInt.of(Runtime.version().feature()) : realm.release();
        var moduleSourcePath = realm.moduleSourcePath();
        var modulePath = project.toModulePath(realm);
        var version = project.descriptor().version();
        var patches = realm.patches((other, unit) -> List.of(project.toModularJar(other, unit)));
        var javac =
            tool(
                "javac",
                new Util.Args()
                    .add("--module", module)
                    .add(version, (args, v) -> args.add("--module-version", v))
                    .add(enablePreview, "--enable-preview")
                    .add(release.isPresent(), "--release", release.orElse(-1))
                    .add("--module-source-path", moduleSourcePath)
                    .add(!modulePath.isEmpty(), "--module-path", modulePath)
                    .forEach(patches, (args, patch) -> args.add("--patch-module", patch))
                    .add("-d", classes)
                    .toStrings());
        return sequence("Compile " + realm.name() + " realm", javac, packageRealm(realm));
      }

      public Task packageRealm(Project.Realm realm) {
        var jars = new ArrayList<Task>();
        var paths = project.paths();
        var modules = paths.modules(realm);
        var sources = paths.sources(realm);
        for (var unit : realm.units().values()) {
          var module = unit.name();
          var classes = paths.classes(realm).resolve(module);
          var jar =
              tool(
                  "jar",
                  new Util.Args()
                      .add("--create")
                      .add("--file", project.toModularJar(realm, unit))
                      .add(verbose, "--verbose")
                      .add(true, "-C", classes, ".")
                      .forEach(unit.resources(), (cmd, path) -> cmd.add(true, "-C", path, "."))
                      .toStrings());
          var src =
              tool(
                  "jar",
                  new Util.Args()
                      .add("--create")
                      .add("--file", sources.resolve(project.toJarName(unit, "sources")))
                      .add(verbose, "--verbose")
                      .add("--no-manifest")
                      .forEach(
                          unit.sources(Project.Source::path),
                          (cmd, path) -> cmd.add(true, "-C", path, "."))
                      .forEach(unit.resources(), (cmd, path) -> cmd.add(true, "-C", path, "."))
                      .toStrings());
          jars.add(jar);
          jars.add(src);
        }
        var name = realm.name();
        return sequence(
            "Package " + name + " modules and sources",
            new Task.CreateDirectories(modules),
            new Task.CreateDirectories(sources),
            parallel("Jar each " + name + " module", jars.toArray(Task[]::new)));
      }

      public Task launchAllTests() {
        var launches =
            project.structure().realms().stream()
                .filter(candidate -> candidate.test(Project.Realm.Modifier.LAUNCH_TESTS))
                .map(this::launchTests)
                .toArray(Task[]::new);
        return sequence("Launch all tests", launches);
      }

      public Task launchTests(Project.Realm realm) {
        var tasks = new ArrayList<Task>();
        for (var unit : realm.units().values()) {
          var module = unit.name();
          var tester = new ToolTester(realm, unit);
          tasks.add(new Task.RunTool("Run provided tool: test(" + module + ")", tester));
          var junit = new JUnitTester(realm, unit);
          var args =
              new Util.Args()
                  .add("--select-module", module)
                  .add("--details", "tree")
                  .add("--details-theme", "unicode")
                  .add("--disable-ansi-colors")
                  .add("--reports-dir", project.paths().out("junit-reports", module));
          tasks.add(new Task.RunTool("Run JUnit Platform for " + module, junit, args.toStrings()));
        }
        return sequence("Launch tests in " + realm.name() + " realm", tasks.toArray(Task[]::new));
      }

      public Task compileApiDocumentation() {
        var realms = project.structure().realms();
        if (realms.isEmpty()) return sequence("Cannot generate API documentation: 0 realms");
        var realm =
            realms.stream()
                .filter(candidate -> candidate.test(Project.Realm.Modifier.CREATE_JAVADOC))
                .findFirst();
        if (realm.isEmpty()) return sequence("No realm wants javadoc: " + realms);
        return compileApiDocumentation(realm.get());
      }

      public Task compileApiDocumentation(Project.Realm realm) {
        var module = project.toModule(realm);
        if (module.isEmpty()) return sequence("Cannot generate API documentation: 0 modules");
        var name = project.descriptor().name();
        var version = project.descriptor().version();
        var file = name + version.map(v -> "-" + v).orElse("");
        var moduleSourcePath = realm.moduleSourcePath();
        var modulePath = project.toModulePath(realm);
        var javadoc = project.paths().javadoc();
        return sequence(
            "Generate API documentation and jar generated site",
            new Task.CreateDirectories(javadoc),
            tool(
                "javadoc",
                new Util.Args()
                    .add("--module", module)
                    .add("--module-source-path", moduleSourcePath)
                    .add(!modulePath.isEmpty(), "--module-path", modulePath)
                    .add("-d", javadoc)
                    .add(!verbose, "-quiet")
                    .add("-Xdoclint:-missing")
                    .toStrings()),
            tool(
                "jar",
                new Util.Args()
                    .add("--create")
                    .add("--file", javadoc.getParent().resolve(file + "-javadoc.jar"))
                    .add(verbose, "--verbose")
                    .add("--no-manifest")
                    .add("-C", javadoc)
                    .add(".")
                    .toStrings()));
      }

      /** Test launcher base. */
      abstract class AbstractTester implements ToolProvider, GarbageCollect {

        private final String name;
        final Project.Realm realm;
        final Project.Unit unit;

        AbstractTester(String name, Project.Realm realm, Project.Unit unit) {
          this.name = name;
          this.realm = realm;
          this.unit = unit;
        }

        @Override
        public String name() {
          return name;
        }

        Stream<ToolProvider> tools() {
          var modulePath = new ArrayList<Path>();
          modulePath.add(project.toModularJar(realm, unit)); // test module first
          modulePath.addAll(project.toModulePaths(realm)); // compile dependencies next, like "main"
          modulePath.add(project.paths().modules(realm)); // same realm last, like "test"
          var layer = Util.Modules.layer(modulePath, unit.name());
          var tools = ServiceLoader.load(layer, ToolProvider.class);
          return StreamSupport.stream(tools.spliterator(), false);
        }

        int run(ToolProvider tool, PrintWriter out, PrintWriter err, String... args) {
          var toolLoader = tool.getClass().getClassLoader();
          var currentThread = Thread.currentThread();
          var currentContextLoader = currentThread.getContextClassLoader();
          currentThread.setContextClassLoader(toolLoader);
          try {
            return tool.run(out, err, args);
          } finally {
            currentThread.setContextClassLoader(currentContextLoader);
          }
        }
      }

      /** Test launcher running provided test tools. */
      class ToolTester extends AbstractTester {

        ToolTester(Project.Realm realm, Project.Unit unit) {
          super("tester(" + unit.name() + ")", realm, unit);
        }

        @Override
        public int run(PrintWriter out, PrintWriter err, String... args) {
          var name = "test(" + unit.name() + ")";
          return tools()
              .filter(tool -> name.equals(tool.name()))
              .mapToInt(tool -> Math.abs(run(tool, out, err, args)))
              .sum();
        }
      }

      /** Test launcher running JUnit Platform. */
      class JUnitTester extends AbstractTester {

        JUnitTester(Project.Realm realm, Project.Unit unit) {
          super("junit(" + unit.name() + ")", realm, unit);
        }

        @Override
        public int run(PrintWriter out, PrintWriter err, String... args) {
          var junit = tools().filter(tool -> "junit".equals(tool.name())).findFirst();
          if (junit.isEmpty()) {
            out.println("Tool named 'junit' not found");
            return 0;
          }
          return run(junit.get(), out, err, args);
        }
      }

      /** Determine and load missing library modules. */
      class Resolver extends Task {

        private final Project.ModuleSurvey systemModulesSurvey;

        public Resolver() {
          super("Resolve missing modules", false, List.of());
          this.systemModulesSurvey = Project.ModuleSurvey.of(ModuleFinder.ofSystem());
        }

        @Override
        public Result execute(Execution execution) {
          var units = project.units();
          var lib = project.paths().lib();
          var projectModulesSurvey = project.structure().survey();
          var libraryModulesSurvey = Project.ModuleSurvey.of(ModuleFinder.of(lib));

          execution.debug("Project modules survey of %s unit(s) -> %s", units.size(), units);
          execution.debug("  declared -> " + projectModulesSurvey.declaredModules());
          execution.debug("  requires -> " + projectModulesSurvey.requiredModules());
          execution.debug("Library modules survey of -> %s", lib.toUri());
          execution.debug("  declared -> " + libraryModulesSurvey.declaredModules());
          execution.debug("  requires -> " + libraryModulesSurvey.requiredModules());
          execution.debug("System with %d modules.", systemModulesSurvey.declaredModules().size());

          try {
            var missing = findMissingModules(libraryModulesSurvey);
            if (missing.isEmpty()) {
              execution.debug("All required modules are locatable.");
              return execution.ok();
            }
            new Loader(execution, lib).resolve(missing);
            return execution.ok();
          } catch (Exception e) {
            return execution.failed(e);
          }
        }

        Map<String, Version> findMissingModules(Project.ModuleSurvey libraryModulesSurvey) {
          var projectModulesSurvey = project.structure().survey();

          var missing = new TreeMap<String, Version>();
          missing.putAll(projectModulesSurvey.requiredModules());
          missing.putAll(libraryModulesSurvey.requiredModules());
          for (var requires : project.descriptor().requires()) {
            missing.put(requires.name(), requires.compiledVersion().orElse(null));
          }

          if (project.library().test(Project.Library.Modifier.ADD_MISSING_JUNIT_TEST_ENGINES))
            Convention.addMissingJUnitTestEngines(missing);
          if (project.library().test(Project.Library.Modifier.ADD_MISSING_JUNIT_PLATFORM_CONSOLE))
            Convention.addJUnitPlatformConsole(missing);

          projectModulesSurvey.declaredModules().forEach(missing::remove);
          libraryModulesSurvey.declaredModules().forEach(missing::remove);
          systemModulesSurvey.declaredModules().forEach(missing::remove);
          return missing;
        }

        private class Loader {

          private final Task.Execution execution;
          private final Path lib;

          private Loader(Execution execution, Path lib) {
            this.execution = execution;
            this.lib = lib;
          }

          void resolve(Map<String, Version> modules) throws Exception {
            var loaded = new ArrayList<String>();
            var repeat = project.library().test(Project.Library.Modifier.RESOLVE_RECURSIVELY);
            do {
              var intersection = new TreeSet<>(modules.keySet());
              load(modules);
              loaded.addAll(modules.keySet());
              modules.clear();
              var libraryModulesSurvey = Project.ModuleSurvey.of(ModuleFinder.of(lib));
              modules.putAll(libraryModulesSurvey.requiredModules());
              libraryModulesSurvey.declaredModules().forEach(modules::remove);
              systemModulesSurvey.declaredModules().forEach(modules::remove);
              intersection.retainAll(modules.keySet());
              if (!intersection.isEmpty())
                throw new IllegalStateException("Unresolved modules: " + intersection);
            } while (repeat && !modules.isEmpty());
            execution.debug("Resolved %d missing modules", loaded.size());
            loaded.stream().sorted().forEach(module -> execution.debug("  - %s", module));
          }

          void load(Map<String, Version> modules) throws Exception {
            execution.debug("Resolve modules: %s", modules);
            for (var entry : modules.entrySet()) {
              var module = entry.getKey();
              var version = entry.getValue();
              var mapping = mapping(module, version);
              var source = mapping.uri();
              var target = lib.resolve(module + "-" + mapping.version() + ".jar");
              execution.bach().atomics().uris().copy(source, target);
              execution.debug("%s <- %s", target, source);
            }
          }

          private Project.ModuleMapper.Mapping mapping(String module, Version version) {
            try {
              return project.library().mapper().apply(module, version);
            } catch (Util.Modules.UnmappedModuleException e) {
              execution.debug("Module %s not mapped directly", module);
              return execution.bach().atomics().mapper().apply(module, version);
            }
          }
        }
      }
    }

    /** An executable task and a potentially non-empty list of sub-tasks. */
    class Task {

      /** Task execution context. */
      public static final class Execution {

        private final Bach bach;
        private final Instant start;
        private final StringWriter out;
        private final StringWriter err;

        public Execution(Bach bach) {
          this.bach = bach;
          this.start = Instant.now();
          this.out = new StringWriter();
          this.err = new StringWriter();
        }

        public Bach bach() {
          return bach;
        }

        public Instant start() {
          return start;
        }

        /** Print message if verbose flag is set. */
        public void debug(String format, Object... args) {
          if (bach().verbose()) {
            out.write(String.format(format, args));
            out.write('\n');
          }
        }

        /** Create result with code zero and empty output strings. */
        public Result ok() {
          return new Result(start, 0, out.toString(), err.toString());
        }

        /** Create result with error code one and append throwable's message to the error string. */
        public Result failed(Throwable throwable) {
          return new Result(start, 1, out.toString(), err.toString() + '\n' + throwable.toString());
        }
      }

      /** Tool-running task. */
      public static final class RunTool extends Task {

        private final ToolProvider[] tool;
        private final String[] args;
        private final String markdown;

        public RunTool(String caption, ToolProvider tool, String... args) {
          super(caption, false, List.of());
          this.tool = new ToolProvider[] {tool};
          this.args = args;
          var arguments = args.length == 0 ? "" : ' ' + String.join(" ", args);
          this.markdown = '`' + tool.name() + arguments + '`';
        }

        @Override
        public Result execute(Execution context) {
          var out = new StringWriter();
          var err = new StringWriter();
          var now = context.start();
          var code = tool[0].run(new PrintWriter(out), new PrintWriter(err), args);
          if (tool[0] instanceof GarbageCollect) {
            tool[0] = null;
            System.gc();
          }
          return new Result(now, code, out.toString(), err.toString());
        }

        @Override
        public String toMarkdown() {
          return markdown;
        }
      }

      /** Creates a directory by creating all nonexistent parent directories first. */
      public static final class CreateDirectories extends Task {

        private final Path path;

        public CreateDirectories(Path path) {
          super("Create directories " + path, false, List.of());
          this.path = path;
        }

        @Override
        public Result execute(Execution context) {
          try {
            Files.createDirectories(path);
            return context.ok();
          } catch (IOException e) {
            return context.failed(e);
          }
        }

        @Override
        public String toMarkdown() {
          return "`Files.createDirectories(Path.of(" + path + "))`";
        }
      }

      private final String caption;
      private final boolean parallel;
      private final List<Task> children;

      /** Initialize a task container */
      public Task(String caption, boolean parallel, List<Task> children) {
        this.caption = caption;
        this.parallel = parallel;
        this.children = children;
      }

      /** Default computation called before executing child tasks. */
      public Result execute(Execution context) {
        return context.ok();
      }

      /** Return markdown representation of this task instance. */
      public String toMarkdown() {
        return caption;
      }
    }

    /** Execution result record. */
    final class Result {
      private final Instant start;
      private final int code;
      private final String out;
      private final String err;

      public Result(Instant start, int code, String out, String err) {
        this.start = start;
        this.code = code;
        this.out = out;
        this.err = err;
      }
    }

    /** Build summary. */
    final class Summary implements Build {

      /** Task and its result tuple. */
      public static final class Detail {
        private final String caption;
        private final Task task;
        private final Result result;

        public Detail(String caption, Task task, Result result) {
          this.caption = caption;
          this.task = task;
          this.result = result;
        }
      }

      private final Project project;
      private final Deque<String> executions = new ConcurrentLinkedDeque<>();
      private final Deque<Detail> details = new ConcurrentLinkedDeque<>();
      private final AssertionError error = new AssertionError("Build failed");

      public Summary(Project project) {
        this.project = project;
      }

      public Project project() {
        return project;
      }

      public void assertSuccessful() {
        var exceptions = error.getSuppressed();
        if (exceptions.length == 0) return;
        var one = exceptions[0]; // first suppressed exception
        if (exceptions.length == 1 && one instanceof RuntimeException) throw (RuntimeException) one;
        throw error;
      }

      /** Task execution is about to begin callback. */
      private void executionBegin(Task task) {
        if (task.children.isEmpty()) return;
        var format = "|   +|%6X|        | %s";
        var thread = Thread.currentThread().getId();
        var text = task.caption;
        executions.add(String.format(format, thread, text));
      }

      /** Task execution ended callback. */
      private void executionEnd(Task task, Result result) {
        var format = "|%4c|%6X|%8d| %s";
        var kind = task.children.isEmpty() ? result.code == 0 ? ' ' : 'X' : '=';
        var thread = Thread.currentThread().getId();
        var millis = Duration.between(result.start, Instant.now()).toMillis();
        var caption = task.children.isEmpty() ? "**" + task.caption + "**" : task.caption;
        var row = String.format(format, kind, thread, millis, caption);
        if (task.children.isEmpty()) {
          var hash = Integer.toHexString(System.identityHashCode(task));
          var detail = new Detail("Task Execution Details " + hash, task, result);
          executions.add(row + " [...](#task-execution-details-" + hash + ")");
          details.add(detail);
        } else {
          executions.add(row);
        }
      }

      public List<String> toMarkdown() {
        var md = new ArrayList<String>();
        md.add("# Summary");
        md.addAll(projectDescription());
        md.addAll(taskExecutionOverview());
        md.addAll(taskExecutionDetails());
        md.addAll(exceptionDetails());
        md.addAll(systemProperties());
        return md;
      }

      private List<String> projectDescription() {
        var descriptor = project.descriptor();
        var md = new ArrayList<String>();
        md.add("");
        md.add("## Project");
        md.add("- name: " + descriptor.name());
        md.add("- version: " + descriptor.version().map(Version::toString).orElse("_none_"));
        md.add("");
        md.add("```text");
        project.print(md::add);
        md.add("```");
        return md;
      }

      private List<String> taskExecutionOverview() {
        var md = new ArrayList<String>();
        md.add("");
        md.add("## Task Execution Overview");
        md.add("|    |Thread|Duration|Caption");
        md.add("|----|-----:|-------:|-------");
        md.addAll(executions);
        md.add("");
        md.add("Legend");
        md.add(" - A row starting with `+` denotes the start of a task container.");
        md.add(" - A blank row start (` `) is a normal task execution. Its caption is emphasized.");
        md.add(" - A row starting with `X` marks an erroneous task execution.");
        md.add(" - A row starting with `=` marks the end (sum) of a task container.");
        md.add(" - The Thread column shows the thread identifier, with `1` denoting main thread.");
        md.add(" - Duration is measured in milliseconds.");
        return md;
      }

      private List<String> taskExecutionDetails() {
        if (details.isEmpty()) return List.of();
        var md = new ArrayList<String>();
        md.add("");
        md.add("## Task Execution Details");
        md.add("");
        for (var detail : details) {
          var caption = detail.caption;
          var task = detail.task;
          var result = detail.result;
          md.add("### " + caption);
          md.add(" - Command = " + task.toMarkdown());
          md.add(" - Start Instant = " + result.start);
          md.add(" - Exit Code = " + result.code);
          md.add("");
          if (!detail.result.out.isBlank()) {
            md.add("Normal (expected) output");
            md.add("```");
            md.add(result.out.strip());
            md.add("```");
          }
          if (!detail.result.err.isBlank()) {
            md.add("Error output");
            md.add("```");
            md.add(result.err.strip());
            md.add("```");
          }
        }
        return md;
      }

      private List<String> exceptionDetails() {
        var exceptions = error.getSuppressed();
        if (exceptions.length == 0) return List.of();
        var md = new ArrayList<String>();
        md.add("");
        md.add("## Exception Details");
        md.add("");
        md.add("- Caught " + exceptions.length + " exception(s).");
        md.add("");
        for (var exception : exceptions) {
          md.add("### " + exception.getMessage());
          md.add("```text");
          var stackTrace = new StringWriter();
          exception.printStackTrace(new PrintWriter(stackTrace));
          md.add(stackTrace.toString());
          md.add("```");
        }
        return md;
      }

      private List<String> systemProperties() {
        var md = new ArrayList<String>();
        md.add("");
        md.add("## System Properties");
        System.getProperties().stringPropertyNames().stream()
            .sorted()
            .forEach(key -> md.add(String.format("- `%s`: `%s`", key, systemProperty(key))));
        return md;
      }

      private String systemProperty(String systemPropertyKey) {
        var value = System.getProperty(systemPropertyKey);
        if (!"line.separator".equals(systemPropertyKey)) return value;
        var build = new StringBuilder();
        for (char c : value.toCharArray()) {
          build.append("0x").append(Integer.toHexString(c).toUpperCase());
        }
        return build.toString();
      }

      public Path write() {
        var markdown = toMarkdown();
        try {
          var directory = project.paths().out();
          return Files.write(directory.resolve("summary.md"), markdown);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    }

    /** Indicates an error happened while executing a task. */
    class TaskExecutionException extends RuntimeException {
      public TaskExecutionException(String message) {
        super(message);
      }
    }

    /** Named tool not available. */
    class ToolNotFoundException extends RuntimeException {
      public ToolNotFoundException(String name) {
        super("No tool with name '" + name + "' available.");
      }
    }
  }

  /** Namespace for common utilities. */
  public interface Util {
    /** Arguments collector. */
    class Args {
      private final List<String> list = new ArrayList<>();

      /** Append a single non-null argument. */
      public Args add(Object arg) {
        list.add(arg.toString());
        return this;
      }

      /** Append two arguments, a key and a value. */
      public Args add(String key, Object arg) {
        return add(key).add(arg);
      }

      /** Conditionally append one or more arguments. */
      public Args add(boolean predicate, Object first, Object... more) {
        return predicate ? add(first).addAll(more) : this;
      }

      /** Conditionally append one or more arguments. */
      @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
      public <T> Args add(Optional<T> optional, BiConsumer<Args, T> visitor) {
        optional.ifPresent(value -> visitor.accept(this, value));
        return this;
      }

      /** Append all given arguments, potentially none. */
      public Args addAll(Object... args) {
        for (var arg : args) add(arg);
        return this;
      }

      /** Walk the given iterable and expect this instance to be changed by side effects. */
      public <T> Args forEach(Iterable<T> iterable, BiConsumer<Args, T> visitor) {
        iterable.forEach(item -> visitor.accept(this, item));
        return this;
      }

      /** Return a copy of the collected argument strings. */
      public List<String> list() {
        return List.copyOf(list);
      }

      @Override
      public String toString() {
        return "Args{" + String.join(", ", list) + '}';
      }

      /** Return a new array of all collected argument strings. */
      String[] toStrings() {
        return list.toArray(String[]::new);
      }
    }

    /** Module-related utilities. */
    interface Modules {

      /**
       * Source patterns matching parts of "Module Declarations" grammar.
       *
       * @see <a href="https://docs.oracle.com/javase/specs/jls/se9/html/jls-7.html#jls-7.7">Module
       *     Declarations</>
       */
      interface Patterns {
        /** Match {@code `module Identifier {. Identifier}`} snippets. */
        Pattern NAME =
            Pattern.compile(
                "(?:module)" // key word
                    + "\\s+([\\w.]+)" // module name
                    + "(?:\\s*/\\*.*\\*/\\s*)?" // optional multi-line comment
                    + "\\s*\\{"); // end marker

        /** Match {@code `requires {RequiresModifier} ModuleName ;`} snippets. */
        Pattern REQUIRES =
            Pattern.compile(
                "(?:requires)" // key word
                    + "(?:\\s+[\\w.]+)?" // optional modifiers
                    + "\\s+([\\w.]+)" // module name
                    + "(?:\\s*/\\*\\s*([\\w.\\-+]+)\\s*\\*/\\s*)?" // optional '/*' version '*/'
                    + "\\s*;"); // end marker
      }

      /** Module descriptor parser. */
      static ModuleDescriptor describe(Path info) {
        var builder = newModule(Strings.readString(info));
        var temporary = builder.build();
        if (temporary.mainClass().isEmpty()) {
          Convention.mainClass(info, temporary.name()).ifPresent(builder::mainClass);
        }
        return builder.build();
      }

      /** Module descriptor parser. */
      static ModuleDescriptor.Builder newModule(String source) {
        // `module Identifier {. Identifier}`
        var nameMatcher = Patterns.NAME.matcher(source);
        if (!nameMatcher.find()) {
          throw new IllegalArgumentException(
              "Expected Java module source unit, but got: " + source);
        }
        var name = nameMatcher.group(1).trim();
        var builder = ModuleDescriptor.newModule(name);
        // "requires module /*version*/;"
        var requiresMatcher = Patterns.REQUIRES.matcher(source);
        while (requiresMatcher.find()) {
          var requiredName = requiresMatcher.group(1);
          Optional.ofNullable(requiresMatcher.group(2))
              .ifPresentOrElse(
                  version -> builder.requires(Set.of(), requiredName, Version.parse(version)),
                  () -> builder.requires(requiredName));
        }
        return builder;
      }

      /** Return module source path by combining a module-info.java path and a module name. */
      static String moduleSourcePath(Path info, String module) {
        var names = new ArrayList<String>();
        var found = new AtomicBoolean(false);
        for (var element : info.subpath(0, info.getNameCount() - 1)) {
          var name = element.toString();
          if (name.equals(module)) {
            if (found.getAndSet(true))
              throw new IllegalArgumentException(
                  String.format("Name '%s' not unique in path: %s", module, info));
            if (names.isEmpty()) names.add("."); // leading '*' are bad
            if (names.size() < info.getNameCount() - 2) names.add("*"); // avoid trailing '*'
            continue;
          }
          names.add(name);
        }
        if (!found.get())
          throw new IllegalArgumentException(
              String.format("Name of module '%s' not found in path's elements: %s", module, info));
        if (names.isEmpty()) return ".";
        return String.join(File.separator, names);
      }

      static ModuleLayer layer(List<Path> modulePath, String module) {
        var finder = ModuleFinder.of(modulePath.toArray(Path[]::new));
        var roots = List.of(module);
        //        if (verbose) {
        //          log.debug("Module path:");
        //          for (var element : modulePath) {
        //            log.debug("  -> %s", element);
        //          }
        //          log.debug("Finder finds module(s):");
        //          finder.findAll().stream()
        //              .sorted(Comparator.comparing(ModuleReference::descriptor))
        //              .forEach(reference -> log.debug("  -> %s", reference));
        //          log.debug("Root module(s):");
        //          for (var root : roots) {
        //            log.debug("  -> %s", root);
        //          }
        //        }
        var boot = ModuleLayer.boot();
        var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), roots);
        var loader = ClassLoader.getPlatformClassLoader();
        var controller =
            ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), loader);
        return controller.layer();
      }

      /** Return modular origin of the given object. */
      static String origin(Object object) {
        var type = object.getClass();
        var module = type.getModule();
        if (module.isNamed()) return module.getDescriptor().toNameAndVersion();
        try {
          return type.getProtectionDomain().getCodeSource().getLocation().toURI().toString();
        } catch (NullPointerException | URISyntaxException ignore) {
          return module.toString();
        }
      }

      /** Unchecked exception thrown when a module name is not mapped. */
      class UnmappedModuleException extends RuntimeException {
        private static final long serialVersionUID = 0L;

        public UnmappedModuleException(String module) {
          super("Module " + module + " is not mapped");
        }
      }
    }

    /** Self-reflecting print support. */
    interface Printable {

      static List<String> print(Object object) {
        var lines = new ArrayList<String>();
        new Printable() {}.print(new Context(object, lines::add, new IdentityHashMap<>()), "");
        return List.copyOf(lines);
      }

      /** Print this instance using the given printer object. */
      default List<String> print() {
        var lines = new ArrayList<String>();
        print(lines::add);
        return List.copyOf(lines);
      }

      /** Print this instance using the given printer object. */
      default void print(Consumer<String> printer) {
        print(new Context(this, printer, new IdentityHashMap<>()), "");
      }

      final class Context {
        private final Object object;
        private final Consumer<String> printer;
        private final Map<Object, AtomicLong> printed;

        private Context(Object object, Consumer<String> printer, Map<Object, AtomicLong> printed) {
          this.object = object;
          this.printer = printer;
          this.printed = printed;
        }

        private Context nested(Object nested) {
          return new Context(nested, printer, printed);
        }
      }

      /** Recursive print method. */
      default void print(Context context, String indent) {
        var object = context.object;
        var printer = context.printer;
        var printed = context.printed;
        var caption = this == object ? printCaption() : object.getClass().getSimpleName();
        var counter = printed.get(this);
        if (counter != null) {
          var count = counter.getAndIncrement();
          printer.accept(String.format("%s# %s already printed (%d)", indent, caption, count));
          return;
        }
        printed.put(this, new AtomicLong(1));
        printer.accept(String.format("%s%s", indent, caption));
        try {
          var fields = object.getClass().getDeclaredFields();
          Arrays.sort(fields, Comparator.comparing(Field::getName));
          for (var field : fields) {
            if (field.isSynthetic()) continue;
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            var name = field.getName();
            try {
              var method = object.getClass().getMethod(name);
              if (!method.getReturnType().equals(field.getType())) continue;
              var value = method.invoke(object);
              print(context, indent, name, value);
            } catch (NoSuchMethodException e) {
              // continue
            }
          }
        } catch (ReflectiveOperationException e) {
          printer.accept(e.getMessage());
        }
      }

      /** Print the given name and its associated value. */
      private void print(Context context, String indent, String name, Object value) {
        var printer = context.printer;
        if (!printTest(name, value)) return;
        if (value instanceof Printable) {
          var type = value.getClass().getTypeName();
          printer.accept(String.format("  %s%s -> instance of %s", indent, name, type));
          ((Printable) value).print(context.nested(value), indent + "  ");
          return;
        }
        if (value instanceof Collection) {
          var collection = (Collection<?>) value;
          if (!collection.isEmpty()) {
            var first = collection.iterator().next();
            if (first instanceof Printable) {
              var size = collection.size();
              var type = value.getClass().getTypeName();
              printer.accept(String.format("  %s%s -> size=%d type=%s", indent, name, size, type));
              for (var element : collection) {
                if (element instanceof Printable) {
                  ((Printable) element).print(context.nested(element), indent + "  ");
                } else printer.accept("Not printable element?! " + element.getClass());
              }
              return;
            }
          }
        }
        printer.accept(String.format("  %s%s = %s", indent, name, printBeautify(value)));
      }

      /** Return beautified String-representation of the given object. */
      default String printBeautify(Object object) {
        if (object == null) return "null";
        if (object.getClass().isArray()) {
          var length = Array.getLength(object);
          var joiner = new StringJoiner(", ", "[", "]");
          for (int i = 0; i < length; i++) joiner.add(printBeautify(Array.get(object, i)));
          return joiner.toString();
        }
        if (object instanceof String) return "\"" + object + "\"";
        if (object instanceof Path) {
          var string = String.valueOf(object);
          if (!string.isEmpty()) return string;
          return "\"" + object + "\" -> " + ((Path) object).toUri();
        }
        if (object instanceof ModuleDescriptor) {
          var module = (ModuleDescriptor) object;
          var joiner = new StringJoiner(", ", "module { ", " }");
          joiner.add("name: " + module.toNameAndVersion());
          joiner.add("requires: " + new TreeSet<>(module.requires()));
          module.mainClass().ifPresent(main -> joiner.add("mainClass: " + main));
          return joiner.toString();
        }
        return String.valueOf(object);
      }

      /** Return caption string of this object. */
      default String printCaption() {
        return getClass().getSimpleName();
      }

      /** Return {@code false} to prevent the named component from being printed. */
      default boolean printTest(String name, Object value) {
        return true;
      }
    }

    /** String-related helpers. */
    interface Strings {

      /** Join a collection of paths to a platform-specific path string. */
      static String join(Collection<Path> paths) {
        return paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
      }

      /** Load all strings from the specified file into the passed properties instance. */
      static Properties load(Properties properties, Path path) {
        if (Files.isRegularFile(path)) {
          try (var reader = Files.newBufferedReader(path)) {
            properties.load(reader);
          } catch (Exception e) {
            throw new RuntimeException("Load properties failed: " + path, e);
          }
        }
        return properties;
      }

      /** Convert all {@link String}-based properties into a {@code Map<String, String>}. */
      static Map<String, String> map(Properties properties) {
        var map = new HashMap<String, String>();
        for (var name : properties.stringPropertyNames()) {
          map.put(name, properties.getProperty(name));
        }
        return Map.copyOf(map);
      }

      /** Read all content from a file into a string. */
      static String readString(Path path) {
        try {
          return Files.readString(path);
        } catch (IOException e) {
          throw new UncheckedIOException("Read all content from file failed: " + path, e);
        }
      }
    }

    /** Uniform Resource Identifier ({@link URI}) read and download support. */
    class Uris {

      private final HttpClient http;

      public Uris() {
        this.http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
      }

      /** Request head-only from the specified uri. */
      public HttpResponse<Void> head(URI uri, int timeout) throws Exception {
        var nobody = HttpRequest.BodyPublishers.noBody();
        var duration = Duration.ofSeconds(timeout);
        var request = HttpRequest.newBuilder(uri).method("HEAD", nobody).timeout(duration).build();
        return http.send(request, HttpResponse.BodyHandlers.discarding());
      }

      /** Copy all content from a uri to a target file. */
      public Path copy(URI uri, Path path, CopyOption... options) throws Exception {
        // logger.log(Level.DEBUG, "Copy {0} to {1}", uri, path);
        Files.createDirectories(path.getParent());
        if ("file".equals(uri.getScheme())) {
          try {
            return Files.copy(Path.of(uri), path, options);
          } catch (Exception e) {
            throw new IllegalArgumentException("copy file failed:" + uri, e);
          }
        }
        var request = HttpRequest.newBuilder(uri).GET();
        if (Files.exists(path) && Files.getFileStore(path).supportsFileAttributeView("user")) {
          try {
            var etagBytes = (byte[]) Files.getAttribute(path, "user:etag");
            var etag = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(etagBytes)).toString();
            request.setHeader("If-None-Match", etag);
          } catch (Exception e) {
            // logger.log(Level.WARNING, "Couldn't get 'user:etag' file attribute: {0}", e);
          }
        }
        var handler = HttpResponse.BodyHandlers.ofFile(path);
        var response = http.send(request.build(), handler);
        if (response.statusCode() == 200) {
          if (Set.of(options).contains(StandardCopyOption.COPY_ATTRIBUTES)) {
            var etagHeader = response.headers().firstValue("etag");
            if (etagHeader.isPresent()) {
              if (Files.getFileStore(path).supportsFileAttributeView("user")) {
                try {
                  var etag = etagHeader.get();
                  Files.setAttribute(path, "user:etag", StandardCharsets.UTF_8.encode(etag));
                } catch (Exception e) {
                  // logger.log(Level.WARNING, "Couldn't set 'user:etag' file attribute: {0}", e);
                }
              }
            } // else logger.log(Level.WARNING, "No etag provided in response: {0}", response);

            var lastModifiedHeader = response.headers().firstValue("last-modified");
            if (lastModifiedHeader.isPresent()) {
              try {
                @SuppressWarnings("SpellCheckingInspection")
                var format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
                var current = System.currentTimeMillis();
                var millis = format.parse(lastModifiedHeader.get()).getTime(); // 0 means "unknown"
                var fileTime = FileTime.fromMillis(millis == 0 ? current : millis);
                Files.setLastModifiedTime(path, fileTime);
              } catch (Exception e) {
                // logger.log(Level.WARNING, "Couldn't set last modified file attribute: {0}", e);
              }
            }
          }
          // logger.log(Level.DEBUG, "{0} <- {1}", path, uri);
          return path;
        }
        if (response.statusCode() == 304 /*Not Modified*/)
          return path; // https://tools.ietf.org/html/rfc7232#section-4.1
        throw new IOException("response=" + response);
      }

      /** Read all content from a uri into a string. */
      public String read(URI uri) throws Exception {
        // logger.log(Level.DEBUG, "Read {0}", uri);
        if ("file".equals(uri.getScheme())) return Files.readString(Path.of(uri));
        var request = HttpRequest.newBuilder(uri).GET();
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString()).body();
      }
    }
  }

  /** Lazily created atomic references to heavyweight objects. */
  private class Atomics {

    private final AtomicReference<Util.Uris> uris;
    private final AtomicReference<Project.ModuleMapper> modules;

    private Atomics() {
      this.uris = new AtomicReference<>();
      this.modules = new AtomicReference<>();
    }

    /** Get or create uris instance. */
    Util.Uris uris() {
      var current = uris.get();
      if (current != null) return current;
      try {
        print(Level.DEBUG, "Create Uris instance");
        var created = new Util.Uris(); // Creates an HttpClient instance with threads...
        return uris.compareAndSet(null, created) ? created : uris.get();
      } catch (Exception e) {
        throw new AssertionError("Create Uris instance failed", e);
      }
    }

    /** Get or create Maven Central "sormuras/modules" ModuleMapper instance. */
    Project.ModuleMapper mapper() {
      var current = modules.get();
      if (current != null) return current;
      try {
        print("Create Maven Central module mapper instance");
        var created = new Project.ModuleMapper.MavenCentral.SormurasModulesMapper(uris());
        return modules.compareAndSet(null, created) ? created : modules.get();
      } catch (Exception e) {
        throw new AssertionError("Create Uris instance failed", e);
      }
    }
  }

  /** Bach.java's main program class. */
  private class Main {

    private final Deque<String> operations;

    /** Initialize this instance with the given command line arguments. */
    private Main(String... arguments) {
      this.operations = new ArrayDeque<>(List.of(arguments));
    }

    /** Run main operation. */
    void run() throws Exception {
      print(Level.DEBUG, "Run main operation(s): " + operations);
      if (operations.isEmpty()) return;
      var operation = operations.removeFirst();
      switch (operation) {
        case "build":
          build();
          return;
        case "help":
          help();
          return;
        case "upgrade":
          upgrade();
          return;
        case "version":
          version();
          return;
        default:
          throw new UnsupportedOperationException(operation);
      }
    }

    /** Print help screen. */
    public void help() {
      print("Bach.java %s running on Java %s", VERSION, Runtime.version());
      print("Usage: java Bach.java <operation> [args...]");
      print("Available operations include:");
      print("  build");
      print("    Build the modular Java project in the current working directory.");
      print("  help");
      print("    Print this help text.");
      print("  upgrade [<version> [<local-file>]]");
      print("    Upgrade local Bach.java source file with the lines of the specified");
      print("    version read directly from https://github.com/sormuras/bach");
      print("    Default version: master, default local file: src/.bach/Bach.java");
      print("  version");
      print("    Print the version of Bach.");
    }

    public void upgrade() throws Exception {
      var source = Path.of(operations.isEmpty() ? "src/.bach/Bach.java" : operations.removeFirst());
      var string = Files.exists(source) ? Files.readString(source) : "";
      print("Source file path: %s", source.toUri());
      print("Source hash code: 0x%X (%d lines)", string.hashCode(), string.lines().count());
      var tag = operations.isEmpty() ? "master" : operations.removeFirst();
      var uri = "https://github.com/sormuras/bach/raw/" + tag + "/src/.bach-11/Bach.java";
      var remote = atomics().uris().read(URI.create(uri));
      print("Remote hash code: 0x%X (%d lines)", remote.hashCode(), remote.lines().count());
      print("Remote file path: %s", uri);
      if (string.hashCode() == remote.hashCode()) return;
      var parent = source.getParent();
      if (Files.notExists(source) && parent != null) Files.createDirectories(parent);
      var target = Files.writeString(source, remote);
      print("Upgrade complete: " + target.toUri());
    }

    /** Print version. */
    public void version() {
      print("" + VERSION);
    }
  }
}
