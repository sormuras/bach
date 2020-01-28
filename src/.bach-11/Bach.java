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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Java Shell Builder.
 *
 * <p>Requires JDK 11 or later.
 */
@SuppressWarnings({"UnusedReturnValue"})
public class Bach {

  /** Version of the Java Shell Builder. */
  static final Version VERSION = Version.parse("11.0-ea");

  /** Default logger instance. */
  private static final Logger LOGGER = System.getLogger("Bach.java");

  /** Entry-point. */
  public static void main(String... args) {
    Build.build(args);
  }

  /** Build the given project. */
  public static Build.Summary build(Consumer<Project.Builder> consumer) {
    var builder = new Project.BuilderFactory(LOGGER, Path.of("")).newProjectBuilder();
    consumer.accept(builder); // side-effects are expected
    var project = builder.newProject();
    return build(project);
  }

  /** Build the given project. */
  public static Build.Summary build(Project project) {
    var printer = Build.Printer.ofSystem();
    return Build.build(project, LOGGER, printer, true, false);
  }

  /** Project model API. */
  public static final class Project {

    private final Path base;
    private final String name;
    private final Version version;
    private final List<Unit> units;
    private final List<Realm> realms;

    public Project(Path base, String name, Version version, List<Unit> units, List<Realm> realms) {
      this.base = base;
      this.name = name;
      this.version = version;
      this.units = List.copyOf(units);
      this.realms = List.copyOf(realms);
    }

    @Override
    public String toString() {
      @SuppressWarnings("StringBufferReplaceableByString")
      var sb = new StringBuilder("Project{");
      sb.append("base=").append(base);
      sb.append(", name='").append(name).append('\'');
      sb.append(", version=").append(version);
      sb.append(", units=").append(units);
      sb.append(", realms=").append(realms);
      sb.append('}');
      return sb.toString();
    }

    /** Project model builder. */
    static class Builder {
      private Path base = Path.of("");
      private String name = "project";
      private Version version = Version.parse("0");
      private List<Unit> units = new ArrayList<>();
      private List<Realm> realms = new ArrayList<>();

      /** Create project instance using property values from this builder. */
      public Project newProject() {
        return new Project(base, name, version, units, realms);
      }

      /** Set project's base directory. */
      public Builder base(Path base) {
        this.base = base;
        return this;
      }

      /** Set project's name. */
      public Builder name(String name) {
        this.name = name;
        return this;
      }

      /** Set project's version. */
      public Builder version(String version) {
        return version(Version.parse(version));
      }

      /** Set project's version. */
      public Builder version(Version version) {
        this.version = version;
        return this;
      }

      /** Set project's units. */
      public Builder units(List<Unit> units) {
        this.units = units;
        return this;
      }

      /** Set project's realms. */
      public Builder realms(List<Realm> realms) {
        this.realms = realms;
        return this;
      }
    }

    /** Directory-based project model builder computer. */
    static class BuilderFactory {

      private final Logger logger;
      private final Path base;

      /** Initialize this scanner instance with a directory to scan. */
      public BuilderFactory(Logger logger, Path base) {
        this.base = base;
        this.logger = logger;
        logger.log(Level.TRACE, "Initialized {0}", this);
      }

      /** Get base directory to be scanned for project properties. */
      public final Path base() {
        return base;
      }

      /** Create default project builder instance and set available properties. */
      public Builder newProjectBuilder() {
        logger.log(Level.DEBUG, "Scanning directory: {0}", base().toAbsolutePath());
        var builder = new Builder();
        builder.base(base());
        scanName().ifPresent(builder::name);
        scanVersion().ifPresent(builder::version);
        var units = scanUnits();
        builder.units(units);
        var sources = scanSources(units);
        builder.realms(scanRealms(units, sources));
        return builder;
      }

      /** Lookup a property value by its key name. */
      public Optional<String> findProperty(String name) {
        var key = "project." + name;
        var property = Optional.ofNullable(System.getProperty(key));
        property.ifPresent(
            value -> logger.log(Level.DEBUG, "System.getProperty(\"{0}\") -> \"{1}\"", key, value));
        return property;
      }

      /** Scan for name property. */
      public Optional<String> scanName() {
        var name = findProperty("name");
        if (name.isPresent()) return name;
        return Optional.ofNullable(base().toAbsolutePath().getFileName()).map(Path::toString);
      }

      /**
       * Scan for version property.
       *
       * <p>Example implementation reading and parsing a version from a {@code .version} file:
       *
       * <pre><code>
       *    public Optional&lt;Version&gt; scanVersion() throws Exception {
       *      var version = base().resolve(".version");
       *      if (Files.notExists(version)) return Optional.empty();
       *      return Optional.of(Version.parse(Files.readString(version)));
       *    }
       * </code></pre>
       */
      public Optional<Version> scanVersion() {
        return findProperty("version").map(Version::parse);
      }

      /** Scan for modular source units. */
      public List<Unit> scanUnits() {
        var src = base.resolve("src"); // More subdirectory candidates? E.g. "modules", "sources"?
        var root = Files.isDirectory(src) ? src : base;
        try (var stream = Files.find(root, 5, (path, __) -> path.endsWith("module-info.java"))) {
          return stream.map(base::relativize).map(Unit::new).collect(Collectors.toList());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }

      /** Scan for common source directories. */
      public List<Path> scanSources(List<Unit> units) {
        var paths = new TreeSet<Path>();
        for (var unit : units) {
          var path = unit.path;
          var name = unit.name();
          var positions = Util.Paths.indexesOf(path, name);
          if (positions.isEmpty())
            throw new IllegalArgumentException("Name '" + name + "' not found in: " + path);
          var index = positions.getLast(); // Or fail if multiple positions are found?
          paths.add(index == 0 ? Path.of(".") : path.subpath(0, index));
        }
        return List.copyOf(paths);
      }

      /** Scan for source directory tree layout. */
      public Layout scanLayout(List<Unit> units) {
        return Layout.find(units)
            .orElseThrow(() -> new RuntimeException("Layout unknown: " + units));
      }

      /** Scan list of modular source units for modular realms. */
      public List<Realm> scanRealms(List<Unit> units, List<Path> sources) {
        return scanLayout(units).realmsOf(units, sources);
      }
    }

    /** Source directory tree layout. */
    public enum Layout {
      /**
       * Single realm source directory tree layout.
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
      JIGSAW {
        @Override
        public Optional<String> realmOf(Unit unit) {
          var path = unit.path;
          var count = path.getNameCount();
          var match = count >= 2 && path.getName(count - 2).toString().equals(unit.name());
          return match ? Optional.of(Realm.DEFAULT_NAME) : Optional.empty();
        }

        @Override
        public List<Realm> realmsOf(List<Unit> units, List<Path> sources) {
          if (units.isEmpty()) return List.of();
          var name = Realm.DEFAULT_NAME;
          var modifiers = EnumSet.of(Realm.Modifier.CREATE_JAVADOC);
          var modules = Unit.toMap(units.stream());
          return List.of(new Realm(name, modifiers, 0, modules, sources, List.of()));
        }
      },

      /**
       * Source tree layout with main and test realm and nested categories.
       *
       * <ul>
       *   <li>{@code ${SRC}/${MODULE}/main/java/module-info.java}
       *   <li>{@code ${SRC}/${MODULE}/test/[java|module]/module-info.java}
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
          var deque = new ArrayDeque<String>();
          unit.path.forEach(name -> deque.addFirst(name.toString()));
          if (!deque.pop().equals("module-info.java"))
            throw new IllegalArgumentException("No module-info.java?! " + unit.path);
          var category = deque.pop();
          if (!(category.equals("java")
              || category.matches("java-\\d+")
              || category.equals("module"))) return Optional.empty();
          var realm = deque.pop();
          if (!(realm.equals("main") || realm.equals("test"))) return Optional.empty();
          var module = deque.pop();
          var match = module.equals(unit.name());
          return match ? Optional.of(realm) : Optional.empty();
        }

        @Override
        public List<Realm> realmsOf(List<Unit> units, List<Path> sources) {
          var main =
              new Realm(
                  "main",
                  EnumSet.of(Realm.Modifier.CREATE_JAVADOC),
                  0,
                  Unit.toMap(units.stream().filter(u -> realmOf(u).orElseThrow().equals("main"))),
                  sources.stream()
                      .map(source -> source.resolve("${MODULE}/main/java"))
                      .collect(Collectors.toList()),
                  List.of());

          var testSources = new ArrayList<Path>();
          for (var source : sources) { // poor man's fan-out/flat-map...
            testSources.add(source.resolve("${MODULE}/test/java"));
            testSources.add(source.resolve("${MODULE}/test/module"));
          }
          var test =
              new Realm(
                  "test",
                  EnumSet.of(Realm.Modifier.ENABLE_PREVIEW, Realm.Modifier.LAUNCH_TESTS),
                  // EnumSet.of(Realm.Modifier.LAUNCH_TESTS),
                  0,
                  Unit.toMap(units.stream().filter(u -> realmOf(u).orElseThrow().equals("test"))),
                  testSources,
                  List.of(main));
          return List.of(main, test);
        }
      };

      /** Test the given modular unit against this layout's expectations. */
      public abstract Optional<String> realmOf(Unit unit);

      /** Create realms based on the passed modular source units. */
      public abstract List<Realm> realmsOf(List<Unit> units, List<Path> sources);

      /** Scan the given units for a well-known modular source layout. */
      public static Optional<Layout> find(List<Unit> units) {
        for (var layout : Set.of(Layout.MAIN_TEST, Layout.JIGSAW))
          if (units.stream().allMatch(unit -> layout.realmOf(unit).isPresent()))
            return Optional.of(layout);
        return Optional.empty();
      }
    }

    /** A module source description unit. */
    public static final class Unit {

      public static Map<String, Unit> toMap(Stream<Unit> units) {
        return units.collect(Collectors.toMap(Unit::name, Function.identity()));
      }

      private final Path path;
      private final ModuleDescriptor descriptor;
      private final List<Source> sources;
      private final List<Path> resources;

      public Unit(Path path) {
        this(path, Util.Modules.describe(path), List.of(Source.of(path.getParent())), List.of());
      }

      public Unit(
          Path path, ModuleDescriptor descriptor, List<Source> sources, List<Path> resources) {
        this.path = path;
        this.descriptor = descriptor;
        this.sources = sources;
        this.resources = resources;
      }

      @Override
      public String toString() {
        @SuppressWarnings("StringBufferReplaceableByString")
        var sb = new StringBuilder("Unit{");
        sb.append("path=").append(path);
        sb.append(", descriptor=").append(descriptor);
        sb.append(", sources=").append(sources);
        sb.append(", resources=").append(resources);
        sb.append(", isMultiRelease=").append(isMultiRelease());
        sb.append(", isMainClassPresent=").append(isMainClassPresent());
        sb.append('}');
        return sb.toString();
      }

      public String name() {
        return descriptor.name();
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
    public static final class Realm {

      /** Realm-related flags controlling the build process. */
      public enum Modifier {
        ENABLE_PREVIEW,
        CREATE_JAVADOC,
        LAUNCH_TESTS
      }

      public static final String DEFAULT_NAME = "default";

      private final String name;
      private final Set<Modifier> modifiers;
      private final int release;
      private final Map<String, Unit> units;
      private final List<Path> sources;
      private final List<Realm> dependencies;

      public Realm(
          String name,
          Set<Modifier> modifiers,
          int release,
          Map<String, Unit> units,
          List<Path> sources,
          List<Realm> dependencies) {
        this.name = name;
        this.modifiers = modifiers.isEmpty() ? Set.of() : EnumSet.copyOf(modifiers);
        this.release = release;
        this.units = Map.copyOf(units);
        this.sources = List.copyOf(sources);
        this.dependencies = List.copyOf(dependencies);
      }

      @Override
      public String toString() {
        @SuppressWarnings("StringBufferReplaceableByString")
        var sb = new StringBuilder("Realm{");
        sb.append("name='").append(name).append('\'');
        sb.append(", units=").append(units);
        sb.append(", sources=").append(sources);
        sb.append(", dependencies=").append(dependencies);
        sb.append('}');
        return sb.toString();
      }

      Path path() {
        return Path.of(name.equals(DEFAULT_NAME) ? "." : name);
      }

      public OptionalInt release() {
        return release == 0 ? OptionalInt.empty() : OptionalInt.of(release);
      }

      /** @return all module names of this realm as a comma-separated string. */
      public String module() {
        return units.values().stream().map(Unit::name).collect(Collectors.joining(","));
      }

      /** Generate {@code --module-source-path} string of this realm. */
      public String moduleSourcePath() {
        return sources.stream()
            .map(Path::toString)
            .map(string -> string.replace("${MODULE}", "*"))
            .collect(Collectors.joining(File.pathSeparator));
      }

      /** Generate {@code --module-path} string of this realm. */
      public String modulePath(Build.Folder folder) {
        var paths = new ArrayList<String>();
        dependencies.stream()
            .map(dependency -> folder.out("modules" /* "classes" */, dependency.name))
            .map(Path::toString)
            .forEach(paths::add);
        paths.add(folder.lib.toString());
        return String.join(File.pathSeparator, paths);
      }

      /** Generate {@code --patch-module} strings for this realm. */
      public List<String> patches(BiFunction<Realm, String, List<Path>> patcher) {
        if (dependencies.isEmpty()) return List.of();
        var patches = new ArrayList<String>();
        for (var unit : units.values()) {
          var module = unit.name();
          for (var dependency : dependencies) {
            var other = dependency.units.get(module);
            if (other == null) continue;
            var paths = Util.Paths.join(patcher.apply(dependency, module));
            patches.add(module + '=' + paths);
          }
        }
        return patches.isEmpty() ? List.of() : List.copyOf(patches);
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
        return modifiers.contains(Source.Modifier.VERSIONED);
      }

      @Override
      public String toString() {
        @SuppressWarnings("StringBufferReplaceableByString")
        var sb = new StringBuilder("Source{");
        sb.append("path=").append(path);
        sb.append(", release=").append(release());
        sb.append(", modifiers=").append(modifiers());
        sb.append(", target=").append(target());
        sb.append(", isVersioned=").append(isVersioned());
        sb.append('}');
        return sb.toString();
      }
    }
  }

  /** Namespace for execution-related types. */
  public static final class Build {

    private final Context context;
    private final Project project;
    private final Plan plan;

    public Build(Context context, Project project, Plan plan) {
      this.context = context;
      this.project = project;
      this.plan = plan;
    }

    /** Supported operation modes by the default build program. */
    enum Operation {
      /** Build the project in the current working directory. */
      BUILD,
      /** Generate, validate, and print project information. */
      DRY_RUN,
      /** Create and execute a single (tool) call on-the-fly. */
      CALL,
      /** Clean all compiled assets. */
      CLEAN,
      /** Print help screen. */
      HELP,
      /** Emit version on the standard output stream and exit. */
      VERSION;

      /** Return the operation for the specified argument. */
      static Operation of(String argument, Operation defaultOperation) {
        if (argument == null) return defaultOperation;
        return valueOf(argument.toUpperCase().replace('-', '_'));
      }
    }

    /** Default build program. */
    static void build(String... args) {
      var arguments = new ArrayDeque<>(List.of(args));
      var operation = Operation.of(arguments.pollFirst(), Operation.DRY_RUN);
      switch (operation) {
        case BUILD:
        case DRY_RUN:
          var factory = new Project.BuilderFactory(LOGGER, Path.of(""));
          var builder = factory.newProjectBuilder();
          var project = builder.newProject();
          var printer = Printer.ofSystem();
          var banner = !arguments.contains("--hide-banner");
          var dryRun = operation == Operation.DRY_RUN || arguments.contains("--dry-run");
          var summary = build(project, LOGGER, printer, banner, dryRun);
          if (summary.throwable != null) throw new Error(summary.throwable.getMessage());
          break;
        case CALL:
          var name = arguments.removeFirst(); // or fail with "cryptic" error message
          var call = Call.of(name, arguments.toArray(String[]::new));
          call.execute(new Context());
          break;
        case CLEAN:
          if (arguments.isEmpty()) Util.Paths.delete(Folder.DEFAULT.out);
          if (arguments.contains("out")) Util.Paths.delete(Folder.DEFAULT.out);
          if (arguments.contains("lib")) Util.Paths.delete(Folder.DEFAULT.lib);
          break;
        case HELP:
          System.out.println("Usage: Bach.java [<operation> [args...]]");
          System.out.println("Operations:");
          for (var constant : Operation.values()) {
            System.out.println("  - " + constant.name().toLowerCase().replace('_', '-'));
          }
          break;
        case VERSION:
          System.out.println(VERSION);
          break;
      }
    }

    /** Build the given project. */
    static Summary build(
        Project project, Logger logger, Printer printer, boolean banner, boolean dryRun) {
      var out = printer.out;

      if (banner) {
        out.accept(Bach.class.getSimpleName() + ' ' + VERSION);
        out.accept("");
      }

      out.accept(project.toString());
      var plan = new Planner(logger, project).newPlan();

      var context = new Context(printer, Context.DEFAULT_LEVELS, true);
      out.accept(context.toString());
      var build = new Build(context, project, plan);
      if (dryRun) {
        out.accept("");
        out.accept("Dry-run successful.");
        return new Summary(build, List.of(), Duration.ZERO, null);
      }

      out.accept("");
      out.accept("Build...");
      var summary = new Executor(build).call();
      out.accept("");
      out.accept("Build took " + summary.duration.toMillis() + " milliseconds.");

      if (banner) {
        out.accept("");
        out.accept("Thanks for using Bach.java · https://github.com/sponsors/sormuras (-:");
      }
      return summary;
    }

    /** Level-aware printer. */
    public static class Printer implements BiConsumer<Level, String> {

      public static Printer ofSystem() {
        return new Printer(System.out::println, System.err::println);
      }

      private final Consumer<String> out;
      private final Consumer<String> err;

      public Printer(Consumer<String> out, Consumer<String> err) {
        this.out = out;
        this.err = err;
      }

      @Override
      public void accept(Level level, String line) {
        (level.getSeverity() <= Level.INFO.getSeverity() ? out : err).accept(line);
      }
    }

    /** Execution context. */
    public static final class Context {

      public static final Set<Level> DEFAULT_LEVELS =
          EnumSet.complementOf(EnumSet.of(Level.ALL, Level.OFF));

      private final Printer printer;
      private final Set<Level> levels;
      private final boolean parallel;

      public Context() {
        this(Printer.ofSystem(), DEFAULT_LEVELS, true);
      }

      public Context(Printer printer, Set<Level> levels, boolean parallel) {
        this.printer = printer;
        this.levels = levels.isEmpty() ? EnumSet.noneOf(Level.class) : EnumSet.copyOf(levels);
        this.parallel = parallel;
      }

      @Override
      public String toString() {
        return "Context{" + "levels=" + levels + ", parallel=" + parallel + '}';
      }

      /** Return true iff the passed call is to be executed. */
      boolean enabled(Call call) {
        if (call.level() == Level.ALL) return true; // ALL as in "always active"
        if (call.level() == Level.OFF) return false; // OFF as in "always skip"
        return levels.contains(call.level());
      }

      /** Return true iff the passed call is to be skipped from execution. */
      final boolean disabled(Call call) {
        return !enabled(call);
      }
    }

    /** Execution event promoter. */
    interface Listener {
      /** Call execution is about to begin callback. */
      default void executionBegin(Call call) {}

      /** Call execution is skipped callback. */
      default void executionDisabled(Call call) {}

      /** Call execution ended callback. */
      default void executionEnd(Call call, Duration duration) {}
    }

    /** A single tool command. */
    interface Call {

      /** Execute this instance. */
      default void executeFiringEvents(Context context, Listener listener) {
        if (context.disabled(this)) {
          listener.executionDisabled(this);
          return;
        }
        listener.executionBegin(this);
        var start = Instant.now();
        try {
          execute(context, listener);
        } finally {
          var duration = Duration.between(start, Instant.now());
          listener.executionEnd(this, duration);
        }
      }

      /** Execute this instance. */
      default void execute(Context context, Listener listener) {
        execute(context);
      }

      /** Execute this instance. */
      void execute(Context context);

      /** Associated execution level. */
      default Level level() {
        return Level.ALL;
      }

      default String toMarkdown() {
        return "`" + toString() + "`";
      }

      default String toJavaLine() {
        return "// not implemented: " + toMarkdown();
      }

      /** Create a named tool call. */
      static Call of(String name, String... args) {
        class Tool implements Call {
          private final ToolProvider tool;
          private final String[] args;

          Tool(ToolProvider tool, String... args) {
            this.tool = tool;
            this.args = args;
          }

          @Override
          public void execute(Context context) {
            var out = new StringWriter();
            var err = new StringWriter();
            var code = tool.run(new PrintWriter(out), new PrintWriter(err), args);
            synchronized (context.printer) {
              var printer = context.printer;
              printer.accept(Level.DEBUG, this.toString());
              print(out, printer, Level.INFO);
              print(err, printer, Level.ERROR);
            }
            if (code != 0) {
              throw new RuntimeException(this + " exit code: " + code);
            }
          }

          private void print(StringWriter source, Printer printer, Level level) {
            var string = source.toString();
            if (string.isEmpty()) return;
            string.lines().forEach(line -> printer.accept(level, "    " + line));
          }

          @Override
          public String toJavaLine() {
            var arguments = args.length == 0 ? "" : ", \"" + String.join("\", \"", args) + "\"";
            return "call(\"" + name + "\"" + arguments.replace('\\', '/') + ")";
          }

          @Override
          public String toString() {
            return tool.name() + (args.length == 0 ? "" : " " + String.join(" ", args));
          }
        }
        var tool = ToolProvider.findFirst(name).orElseThrow();
        return new Tool(tool, args);
      }

      /** Create an active call providing an callable instance. */
      static Call of(Callable<?> callable, String... code) {
        class Lambda implements Call {
          private final Callable<?> callable;
          private final String[] code;

          Lambda(Callable<?> callable, String... code) {
            this.callable = callable;
            this.code = code;
          }

          @Override
          public void execute(Context context) {
            try {
              callable.call();
            } catch (Exception e) {
              throw new RuntimeException(this + " failed: " + e.getMessage(), e);
            }
          }

          @Override
          public String toJavaLine() {
            return String.join(" ", code);
          }

          @Override
          public String toString() {
            return toJavaLine();
          }
        }
        return new Lambda(callable, code);
      }
    }

    /** A composite task that is composed of a name and a list of nested {@link Call} instances. */
    public static final class Plan implements Call {

      private final String caption;
      private final Level level;
      private final boolean parallel;
      private final List<Call> calls;

      public Plan(String caption, Level level, boolean parallel, List<Call> calls) {
        this.caption = caption;
        this.level = level;
        this.parallel = parallel;
        this.calls = calls;
      }

      @Override
      public Level level() {
        return level;
      }

      @Override
      public void execute(Context context, Listener listener) {
        var parallel = context.parallel && this.parallel;
        var stream = parallel ? calls.stream().parallel() : calls.stream();
        stream.forEach(call -> call.executeFiringEvents(context, listener));
      }

      @Override
      public void execute(Context context) {}

      /** Walk a tree of calls starting with this container instance. */
      public int walk(Consumer<Walker> consumer) {
        return walk(this, 0, consumer);
      }

      @Override
      public String toMarkdown() {
        return String.format(
            "%s _(size=%d, level=%s, parallel=%s)_", caption, calls.size(), level(), parallel);
      }

      /** Walk a tree of calls starting with the given call instance. */
      private static int walk(Call call, int depth, Consumer<Walker> consumer) {
        consumer.accept(new Walker(call, depth));
        if (call instanceof Plan) {
          var plan = (Plan) call;
          var count = 0;
          for (var child : plan.calls) count += walk(child, depth + 1, consumer);
          return count;
        }
        return 1;
      }

      /** Walk companion. */
      public static final class Walker {
        private final Call call;
        private final int depth;

        public Walker(Call call, int depth) {
          this.call = call;
          this.depth = depth;
        }

        public String toMarkdown(String indent) {
          return indent.repeat(depth) + "- " + call.toMarkdown();
        }
      }
    }

    /** Folder configuration record. */
    public static final class Folder {

      public static final Folder DEFAULT = new Folder();

      private static Path resolve(Path path, String... more) {
        if (more.length == 0) return path;
        return path.resolve(String.join("/", more)).normalize();
      }

      private final Path out;
      private final Path lib;

      public Folder() {
        this(Path.of(""));
      }

      public Folder(Path base) {
        this(base.resolve(".bach"), base.resolve("lib"));
      }

      public Folder(Path out, Path lib) {
        this.out = out;
        this.lib = lib;
      }

      public Path out(String... more) {
        return resolve(out, more);
      }
    }

    /** Project build container computer. */
    public static class Planner {

      private final Logger logger;
      private final Project project;
      private final Folder folder;

      /** Initialize this planner instance. */
      public Planner(Logger logger, Project project) {
        this(logger, project, new Folder(project.base));
      }

      /** Initialize this planner instance. */
      public Planner(Logger logger, Project project, Folder folder) {
        this.logger = logger;
        this.project = project;
        this.folder = folder;
        logger.log(Level.TRACE, "Initialized {0}", this);
      }

      /** Compute project build plan. */
      public Plan newPlan() {
        logger.log(Level.DEBUG, "Computing build plan for {0}", project);
        logger.log(Level.DEBUG, "Using folder configuration: {0}", folder);
        if (project.units.isEmpty()) return new Plan("No unit!", Level.ALL, false, List.of());
        if (project.realms.isEmpty()) return new Plan("No realm?!", Level.ALL, false, List.of());
        return new Plan(
            "Build " + project.name + " " + project.version,
            Level.ALL,
            false,
            List.of(
                showSystemInformation(),
                createDirectories(folder.out),
                new Plan(
                    "Compile and generate API documentation",
                    Level.ALL,
                    true,
                    List.of(compileAllRealms(), generateApiDocumentation()))));
      }

      /** Print system information. */
      public Call showSystemInformation() {
        return new Plan(
            "Show System Information",
            Level.INFO,
            true,
            List.of(
                Call.of("javac", "--version"),
                Call.of("javadoc", "--version"),
                Call.of("jar", "--version"),
                Call.of("jdeps", "--version")));
      }

      /** Create output directory. */
      public Call createDirectories(Path path) {
        return Call.of(
            () -> Files.createDirectories(path),
            "Files.createDirectories(" + Code.pathOf(path) + ")");
      }

      /** Compile all realms. */
      public Call compileAllRealms() {
        var calls = project.realms.stream().map(this::compileRealm).collect(Collectors.toList());
        return new Plan("Compile all realms", Level.ALL, false, calls);
      }

      /** Compile specified realm. */
      public Call compileRealm(Project.Realm realm) {
        if (realm.units.isEmpty())
          return new Plan("No units in " + realm.name + " realm?!", Level.ALL, false, List.of());
        var classes = folder.out("classes", realm.path().toString());
        var enablePreview = realm.modifiers.contains(Project.Realm.Modifier.ENABLE_PREVIEW);
        var release = enablePreview ? OptionalInt.of(Runtime.version().feature()) : realm.release();
        var moduleSourcePath = realm.moduleSourcePath();
        var modulePath = realm.modulePath(folder);
        var modulePatches =
            realm.patches(
                (dependency, module) ->
                    List.of(
                        folder.out(
                            Path.of(
                                    "modules",
                                    dependency.path().toString(),
                                    module + "-" + project.version + ".jar")
                                .toString())));
        var javac =
            Call.of(
                "javac",
                new Util.Args()
                    .add("--module", realm.module())
                    .add("--module-version", project.version)
                    .add(enablePreview, "--enable-preview")
                    .add(release.isPresent(), "--release", release.orElse(-1))
                    .add("--module-source-path", moduleSourcePath)
                    .add(!modulePath.isEmpty(), "--module-path", modulePath)
                    .forEach(modulePatches, (args, patch) -> args.add("--patch-module", patch))
                    .add("-d", classes)
                    .toStrings());
        return new Plan(
            "Compile " + realm.name + " realm",
            Level.ALL,
            false,
            List.of(javac, packageRealm(realm)));
      }

      public Call packageRealm(Project.Realm realm) {
        var jars = new ArrayList<Call>();
        var realmPath = realm.path().toString();
        var modules = folder.out("modules", realmPath);
        var sources = folder.out("sources", realmPath);
        for (var unit : realm.units.values()) {
          var module = unit.name();
          var file = module + "-" + project.version;
          var classes = folder.out("classes", realmPath, module);
          jars.add(
              Call.of(
                  "jar",
                  new Util.Args()
                      .add("--create")
                      .add("--file", modules.resolve(file + ".jar"))
                      // .add(logger().verbose(), "--verbose")
                      .add(true, "-C", classes, ".")
                      .forEach(
                          realm.dependencies,
                          (args, other) -> {
                            var path = folder.out("classes", other.path().toString(), module);
                            args.add(other.units.containsKey(module), "-C", path.toString(), ".");
                          })
                      .forEach(unit.resources, (cmd, path) -> cmd.add(true, "-C", path, "."))
                      .toStrings()));
          jars.add(
              Call.of(
                  "jar",
                  new Util.Args()
                      .add("--create")
                      .add("--file", sources.resolve(file + "-sources.jar"))
                      // .add(logger().verbose(), "--verbose")
                      .add("--no-manifest")
                      .forEach(
                          unit.sources(Project.Source::path),
                          (cmd, path) -> cmd.add(true, "-C", path, "."))
                      .forEach(unit.resources, (cmd, path) -> cmd.add(true, "-C", path, "."))
                      .toStrings()));
        }
        return new Plan(
            String.format("Package %s modules and sources", realm.name),
            Level.ALL,
            false,
            List.of(
                createDirectories(modules),
                createDirectories(sources),
                new Plan("Parallel jar calls", Level.ALL, true, jars)));
      }

      /** Generate and package modular API documentation. */
      public Call generateApiDocumentation() {
        var realm = project.realms.get(0); // assuming the first one is the one...
        var module = realm.module();
        if (module.isEmpty()) return new Plan("No module?!", Level.ALL, false, List.of());
        var file = project.name + "-" + project.version;
        var moduleSourcePath = realm.moduleSourcePath();
        var modulePath = realm.modulePath(folder);
        var javadoc = folder.out("documentation", "javadoc");
        return new Plan(
            "Generate API documentation and jar generated site",
            Level.ALL,
            false,
            List.of(
                createDirectories(javadoc),
                Call.of(
                    "javadoc",
                    new Util.Args()
                        .add("--module", module)
                        .add("--module-source-path", moduleSourcePath)
                        .add(!modulePath.isEmpty(), "--module-path", modulePath)
                        .add("-d", javadoc)
                        .add("-quiet")
                        .add("-Xdoclint:-missing")
                        .toStrings()),
                Call.of(
                    "jar",
                    new Util.Args()
                        .add("--create")
                        .add("--file", javadoc.getParent().resolve(file + "-javadoc.jar"))
                        // .add(logger().verbose(), "--verbose")
                        .add("--no-manifest")
                        .add("-C", javadoc)
                        .add(".")
                        .toStrings())));
      }
    }

    /** Java source code related helpers. */
    private static final class Code {
      private Code() {}

      /** Convert the string representation of the given object into a Java source snippet. */
      public static String $(Object object) {
        return "\"" + object.toString() + "\"";
      }

      /** Create {@code Path.of("some/path/...")} snippet. */
      public static String pathOf(Path path) {
        return "Path.of(" + $(path.toString().replace('\\', '/')) + ")";
      }
    }

    /** Composition executor. */
    public static class Executor implements Callable<Summary>, Listener {

      private final Build build;
      private final Instant start;
      private final Collection<Call> calls;

      public Executor(Build build) {
        this.build = build;
        this.start = Instant.now();
        this.calls = new ConcurrentLinkedQueue<>();
      }

      /** Compute summary by executing the build. */
      @Override
      public Summary call() {
        try {
          build.plan.executeFiringEvents(build.context, this);
        } catch (RuntimeException exception) {
          return newSummary(exception);
        }
        return newSummary(null);
      }

      private Summary newSummary(Throwable throwable) {
        @SuppressWarnings("SpellCheckingInspection")
        var formatter =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.systemDefault());
        var duration = Duration.between(start, Instant.now());
        out("");
        out("# Build Summary");
        out(" - project = %s", build.project);
        out(" - context = %s", build.context);
        out(" - start = %s", start);
        out(" - duration = %s milliseconds", duration.toMillis());
        out("");
        out("## Plan");
        build.plan.walk(it -> out(it.toMarkdown("  ")));
        out("## Program");
        out("```java");
        out("// default package");
        out("");
        out("import java.nio.file.Files;");
        out("import java.nio.file.Path;");
        out("import java.util.spi.ToolProvider;");
        out("");
        out("class Build" + formatter.format(start.truncatedTo(ChronoUnit.SECONDS)) + " {");
        out("  public static void main(String... args) throws Exception {");
        calls.forEach(call -> out("    %s;", call.toJavaLine()));
        out("  }");
        out("");
        out("  private static void call(String name, String... args) {");
        out("    var tool = ToolProvider.findFirst(name).orElseThrow();");
        out("    System.out.println('\\n' + name + ' ' + String.join(\" \", args));");
        out("    tool.run(System.out, System.err, args);");
        out("  }");
        out("}");
        out("```");
        return new Summary(build, List.copyOf(calls), duration, throwable);
      }

      private void out(String format, Object... args) {
        build.context.printer.out.accept(String.format(format, args));
      }

      @Override
      public void executionBegin(Call call) {
        if (!(call instanceof Plan)) calls.add(call);
      }

      @Override
      public void executionDisabled(Call call) {}

      @Override
      public void executionEnd(Call call, Duration duration) {
        if (call instanceof Plan) out("[%5d ms] for %s", duration.toMillis(), call.toMarkdown());
      }
    }

    /** Execution summary. */
    public static final class Summary {
      private final Build build;
      private final List<Call> calls;
      private final Duration duration;
      private final Throwable throwable;

      public Summary(Build build, List<Call> calls, Duration duration, Throwable throwable) {
        this.build = build;
        this.calls = calls;
        this.duration = duration;
        this.throwable = throwable;
      }

      public Throwable throwable() {
        return throwable;
      }

      @Override
      public String toString() {
        return "Summary{"
            + "build="
            + build
            + ", calls="
            + calls
            + ", duration="
            + duration
            + ", throwable="
            + throwable
            + '}';
      }
    }
  }

  /** Common utility collection. */
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

      /** Return a new array of collected argument strings. */
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
        return describe(Paths.readString(info));
      }

      /** Module descriptor parser. */
      static ModuleDescriptor describe(String source) {
        return newModule(source).build();
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
        var builder = ModuleDescriptor.newModule(name, Set.of(ModuleDescriptor.Modifier.SYNTHETIC));
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
    }

    /** Path-related utilities. */
    interface Paths {

      /** Delete the given path recursively. */
      static Path delete(Path path) {
        return delete(path, __ -> true);
      }

      /** Delete the given path recursively. */
      static Path delete(Path path, Predicate<Path> filter) {
        try { // trivial case: delete existing empty directory or single file
          Files.deleteIfExists(path);
          return path;
        } catch (DirectoryNotEmptyException ignored) {
          // fall-through
        } catch (Exception e) {
          throw new RuntimeException("Delete path failed: " + path, e);
        }
        try (var stream = Files.walk(path)) { // default case: walk the tree...
          var streamed = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
          for (var selected : streamed.collect(Collectors.toList())) {
            Files.deleteIfExists(selected);
          }
        } catch (Exception e) {
          throw new RuntimeException("Delete path failed: " + path, e);
        }
        return path;
      }

      /** Find all index positions of the specified name within the given path. */
      static Deque<Integer> indexesOf(Path path, String name) {
        var deque = new ArrayDeque<Integer>();
        for (int i = 0; i < path.getNameCount(); i++) {
          if (name.equals(path.getName(i).toString())) deque.add(i);
        }
        return deque;
      }

      /** Join a collection of paths to a platform-specific path string. */
      static String join(Collection<Path> paths) {
        return paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
      }

      /** Read all content from a file into a string. */
      static String readString(Path path) {
        try {
          return Files.readString(path);
        } catch (Exception e) {
          throw new RuntimeException("Read all content from file failed: " + path, e);
        }
      }
    }
  }

  /** Hidden default constructor. */
  private Bach() {}
}
