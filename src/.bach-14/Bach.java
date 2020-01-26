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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/**
 * Java Shell Builder.
 *
 * <p>Requires JDK 14 with "--enable-preview".
 */
@SuppressWarnings({"UnusedReturnValue"})
public class Bach {

  /** Version of the Java Shell Builder. */
  static final Version VERSION = Version.parse("1-ea");

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
  public record Project(Path base, String name, Version version, List<Realm> realms) {

    /** Project model builder. */
    static class Builder {
      private Path base = Path.of("");
      private String name = "project";
      private Version version = Version.parse("0");
      private List<Realm> realms = new ArrayList<>();

      /** Create project instance using property values from this builder. */
      public Project newProject() {
        return new Project(base, name, version, realms);
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
        builder.realms(List.of(new Realm("default", List.of("default"), base().resolve("src").toString(), null)));
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
    }

    /** A realm of sources. */
    record Realm(String name, List<String> modules, String moduleSourcePath, String modulePath) {
      Path path() {
        return Path.of(name.equals("default") ? "." : name);
      }
    }
  }

  /** Namespace for execution-related types. */
  public record Build(Context context, Project project, Plan plan) {

    /** Supported operation modes by the default build program. */
    enum Operation {
      /** Build the project in the current working directory. */
      BUILD,
      /** Create and execute a single (tool) call on-the-fly. */
      CALL,
      /** Generate, validate, and print project information. */
      DRY_RUN,
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
        case BUILD, DRY_RUN -> {
          var factory = new Project.BuilderFactory(LOGGER, Path.of(""));
          var builder = factory.newProjectBuilder();
          var project = builder.newProject();
          var printer = Printer.ofSystem();
          var banner = !arguments.contains("--hide-banner");
          var dryRun = operation == Operation.DRY_RUN || arguments.contains("--dry-run");
          build(project, LOGGER, printer, banner, dryRun);
        }
        case CALL -> {
          var name = arguments.removeFirst(); // or fail with "cryptic" error message
          var call = Call.of(name, arguments.toArray(String[]::new));
          call.executeNow(new Context());
        }
        case VERSION -> System.out.println(VERSION);
      }
    }

    /** Build the given project. */
    static Summary build(
        Project project, Logger logger, Printer printer, boolean banner, boolean dryRun) {
      var out = printer.out;

      if (banner) {
        out.accept(Bach14.class.getSimpleName() + ' ' + VERSION);
        out.accept("");
      }

      out.accept(project.toString());
      var plan = new Planner(logger, project).newPlan();

      out.accept("");
      var count = plan.walk(it -> out.accept(it.toMarkdown("  ")));
      out.accept("The generated call plan contains " + count + " plain calls (leaves).");

      if (dryRun) {
        out.accept("");
        out.accept("Dry-run successful.");
        return new Summary(project);
      }

      out.accept("");
      out.accept("Build...");
      var context = new Context(printer, Context.DEFAULT_LEVELS, true);

      out.accept(context.toString());
      var build = new Build(context, project, plan);
      var summary = new Executor(build).call();

      out.accept("");
      summary.calls().forEach(call -> out.accept(call.toMarkdown()));
      out.accept("");
      out.accept("Build took " + summary.duration().toMillis() + " milliseconds.");

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
    record Context(Printer printer, Set<Level> levels, boolean parallel) {

      public static final Set<Level> DEFAULT_LEVELS = EnumSet.complementOf(EnumSet.of(Level.ALL, Level.OFF));

      public Context() {
        this(Printer.ofSystem(), DEFAULT_LEVELS, true);
      }

      public Context {
        this.levels = levels.isEmpty() ? EnumSet.noneOf(Level.class) : EnumSet.copyOf(levels);
      }

      /** Return true iff the passed call is to be executed. */
      boolean enabled(Call call) {
        if (call.level() == Level.ALL) return true; // ALL as in "always active"
        if (call.level() == Level.OFF) return false; // OFF as in "always skip"
        return levels().contains(call.level());
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
      default void execute(Context context, Listener listener) {
        if (context.disabled(this)) {
          listener.executionDisabled(this);
          return;
        }
        listener.executionBegin(this);
        var start = Instant.now();
        try {
          executeNow(context, listener);
        } finally {
          var duration = Duration.between(Instant.now(), start);
          listener.executionEnd(this, duration);
        }
      }

      /** Execute this instance. */
      default void executeNow(Context context, Listener listener) {
        executeNow(context);
      }

      /** Execute this instance. */
      void executeNow(Context context);

      /** Associated execution level. */
      default Level level() {
        return Level.ALL;
      }

      default String toMarkdown() {
        return "· `" + toString() + "`";
      }

      /** Create a named tool call. */
      static Call of(String name, String... args) {
        record Tool(ToolProvider tool, String... args) implements Call {
          @Override
          public void executeNow(Context context) {
            var out = new StringWriter();
            var err = new StringWriter();
            var code = tool.run(new PrintWriter(out), new PrintWriter(err), args);
            synchronized (context.printer()) {
              var printer = context.printer();
              printer.accept(Level.DEBUG, this.toString());
              print(out, printer, Level.INFO);
              print(err, printer, Level.ERROR);
            }
            if (code != 0) {
              throw new RuntimeException(tool.name() + " exit code: " + code);
            }
          }

          private void print(StringWriter source, Printer printer, Level level) {
            var string = source.toString();
            if (string.isEmpty()) return;
            string.lines().forEach(line -> printer.accept(level,"    " + line));

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
        record Lambda(Callable<?> callable, String... code) implements Call {
          @Override
          public void executeNow(Context context) {
            try {
              callable.call();
            } catch (Exception e) {
              throw new RuntimeException(this + " failed: " + e.getMessage(), e);
            }
          }

          @Override
          public String toString() {
            return String.join(" ", code);
          }
        }
        return new Lambda(callable, code);
      }
    }

    /** A composite task that is composed of a name and a list of nested {@link Call} instances. */
    record Plan(String caption, Level level, boolean parallel, List<Call> calls) implements Call {

      @Override
      public void executeNow(Context context, Listener listener) {
        var parallel = context.parallel() && parallel();
        var stream = parallel ? calls().stream().parallel() : calls().stream();
        stream.forEach(call -> call.execute(context, listener));
      }

      @Override
      public void executeNow(Context context) {}

      /** Walk a tree of calls starting with this container instance. */
      public int walk(Consumer<Walker> consumer) {
        return walk(this, 0, consumer);
      }

      @Override
      public String toMarkdown() {
        return String.format(
            "» %s _(size=%d, level=%s, parallel=%s)_",
            caption(), calls.size(), level(), parallel());
      }

      /** Walk a tree of calls starting with the given call instance. */
      private static int walk(Call call, int depth, Consumer<Walker> consumer) {
        consumer.accept(new Walker(call, depth));
        if (call instanceof Plan plan) {
          var count = 0;
          for (var child : plan.calls()) count += walk(child, depth + 1, consumer);
          return count;
        }
        return 1;
      }

      /** Walk companion. */
      public record Walker(Call call, int depth) {
        public String toMarkdown(String indent) {
          return indent.repeat(depth) + "- " + call.toMarkdown();
        }
      }
    }

    /** Folder configuration record. */
    record Folder(Path out, Path lib) {
      public Folder(Path base) {
        this(base.resolve(".bach"), base.resolve("lib"));
      }
    }

    /** Project build container computer. */
    public static class Planner {

      private final Logger logger;
      private final Project project;
      private final Folder folder;

      /** Initialize this planner instance. */
      public Planner(Logger logger, Project project) {
        this(logger, project, new Folder(project.base()));
      }

      /** Initialize this planner instance. */
      public Planner(Logger logger, Project project, Folder folder) {
        this.logger = logger;
        this.project = project;
        this.folder = folder;
        logger.log(Level.TRACE, "Initialized {0}", this);
      }

      /** Compute project build container. */
      public Plan newPlan() {
        logger.log(Level.DEBUG, "Computing build container for {0}", project);
        logger.log(Level.DEBUG, "Using folder configuration: {0}", folder);
        return new Plan(
            "Build " + project.name() + " " + project.version(),
            Level.ALL,
            false,
            List.of(
                showSystemInformation(),
                createOutputDirectory(),
                compileAllRealms()));
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
      public Call createOutputDirectory() {
        return Call.of(
            () -> Files.createDirectories(folder.out),
            "Files.createDirectories(" + Code.pathOf(folder.out) + ")");
      }

      /** Compile all realms. */
      public Call compileAllRealms() {
        var calls =
            project.realms().stream()
                .map(this::compileRealm)
                .collect(Collectors.toList());
        return new Plan("Compile all realms", Level.ALL, false, calls);
      }

      /** Compile specified realm. */
      public Call compileRealm(Project.Realm realm) {
        var classes = folder.out().resolve("classes").resolve(realm.path()).normalize().toString();
        return new Plan(
            "Compile " + realm.name() + " realm",
            Level.ALL,
            false,
            List.of(
                Call.of(
                    "javac",
                    "--module",
                    String.join(",", realm.modules()),
                    "--module-source-path",
                    realm.moduleSourcePath(),
                    "-d",
                    classes)));
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
    public static class Executor implements Callable<Summary> {

      private final Build build;
      private final Summary summary;

      public Executor(Build build) {
        this.build = build;
        this.summary = new Summary(build.project());
      }

      /** Compute summary by executing the configuration. */
      @Override
      public Summary call() {
        var start = Instant.now();
        build.plan().execute(build.context(), summary);
        summary.duration = Duration.between(start, Instant.now());
        return summary;
      }
    }

    /** Execution summary. */
    public static class Summary implements Listener {
      private final Project project;
      private final Collection<Call> calls;
      private Duration duration;

      public Summary(Project project) {
        this.project = project;
        this.calls = new ConcurrentLinkedQueue<>();
      }

      public List<Call> calls() {
        return List.copyOf(calls);
      }

      public Duration duration() {
        return duration;
      }

      @Override
      public void executionBegin(Call call) {
        if (!(call instanceof Plan)) calls.add(call);
      }

      @Override
      public void executionDisabled(Call call) {}

      @Override
      public void executionEnd(Call call, Duration duration) {}

      @Override
      public String toString() {
        return "Summary for " + project.name() + " -> " + calls.size() + " calls executed";
      }
    }
  }

  /** Hidden default constructor. */
  private Bach() {}
}
