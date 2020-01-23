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
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.spi.ToolProvider;

/**
 * Java Shell Builder.
 *
 * <p>Requires JDK 11 or later.
 */
public class Bach11 {

  /** Version of the Java Shell Builder. */
  static final Version VERSION = Version.parse("1-ea");

  /** Supported operation modes by the default build program. */
  private enum Operation {
    /** Build the project in the current working directory. */
    BUILD,
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
  public static void main(String... args) {
    var arguments = new ArrayDeque<>(List.of(args));
    var operation = Operation.of(arguments.pollFirst(), Operation.DRY_RUN);

    if (operation == Operation.VERSION) {
      System.out.println(VERSION);
      return;
    }

    System.out.println("Bach.java " + VERSION);

    var bach = new Bach11();
    var project = bach.newProject();
    System.out.println("Project:");
    System.out.println(project);
    var plan = bach.newPlan(project);
    System.out.println("Build plan:");
    plan.walk((indent, call) -> System.out.println(indent + "- " + call.toMarkdown()));
    switch (operation) {
      case BUILD:
        var configuration = new Configuration(project, plan);
        var summary = bach.execute(configuration);
        System.out.println(summary.calls().size() + " calls executed:");
        summary.calls().forEach(call -> System.out.println(call.toMarkdown()));
        System.out.println();
        System.out.println("Build took " + summary.duration().toMillis() + " milliseconds.");
        break;
      case DRY_RUN:
        System.out.println();
        System.out.println("Dry-run successful.");
        break;
    }
    System.out.println();
    System.out.println("Thanks for using Bach.java · https://github.com/sponsors/sormuras (-:");
  }

  /** Logger instance. */
  private final Logger logger;

  /** Initialize Java Shell Builder instance with default components. */
  public Bach11() {
    this(System.getLogger("Bach"));
  }

  /** Initialize Java Shell Builder instance canonically. */
  Bach11(Logger logger) {
    this.logger = logger;
    logger.log(Level.TRACE, "Initialized {0}", this);
  }

  /** Create project build for the current working directory. */
  public Project.Builder newProjectBuilder() {
    return new Scanner(Path.of("")).call();
  }

  /** Create project for the current working directory. */
  public Project newProject() {
    return newProjectBuilder().build();
  }

  /** Create call plan for the given project. */
  public Plan newPlan(Project project) {
    return new Planner(project).call();
  }

  /** Execute the given plan. */
  public Summary execute(Configuration configuration) {
    return new Executor(configuration).call();
  }

  /** Return {@code "Bach11 " + }{@link #VERSION}. */
  @Override
  public String toString() {
    return "Bach11 " + VERSION;
  }

  /** Project model API. */
  /*record*/ public static final class Project {

    private final Path base;
    private final String name;
    private final Version version;

    /** Initialize this project instance. */
    public Project(Path base, String name, Version version) {
      this.base = base;
      this.name = name;
      this.version = version;
    }

    /** Get base directory of this project. */
    public Path base() {
      return base;
    }

    /** Get name of this project. */
    public String name() {
      return name;
    }

    /** Get version of this project. */
    public Version version() {
      return version;
    }

    @Override
    public String toString() {
      return "Project {base='" + base() + "', name=\"" + name() + "\", version=" + version() + "}";
    }

    /** Project model builder. */
    public static final class Builder {
      private Path base = Path.of("");
      private String name = "project";
      private Version version = Version.parse("0");

      /** Create project instance using property values from this builder. */
      public Project build() {
        return new Project(base, name, version);
      }

      /** Set project's base directory. */
      public Builder setBase(Path base) {
        this.base = base;
        return this;
      }

      /** Set project's name. */
      public Builder setName(String name) {
        this.name = name;
        return this;
      }

      /** Set project's version. */
      public Builder setVersion(String version) {
        return setVersion(Version.parse(version));
      }

      /** Set project's version. */
      public Builder setVersion(Version version) {
        this.version = version;
        return this;
      }
    }
  }

  /** Directory-based project model builder computer. */
  public class Scanner implements Callable<Project.Builder> {

    private final Path base;

    /** Initialize this scanner instance with a directory to scan. */
    public Scanner(Path base) {
      this.base = base;
      logger.log(Level.TRACE, "Initialized {0}", this);
    }

    /** Get base directory to be scanned for project properties. */
    public final Path base() {
      return base;
    }

    /** Lookup a property value by its key name. */
    public Optional<String> getProperty(String name) {
      var key = "project." + name;
      var property = Optional.ofNullable(System.getProperty(key));
      property.ifPresent(
          value -> logger.log(Level.DEBUG, "System.getProperty(\"{0}\") -> \"{1}\"", key, value));
      return property;
    }

    /** Scan for name property. */
    public Optional<String> scanName() {
      var name = getProperty("name");
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
      return getProperty("version").map(Version::parse);
    }

    /** Create default project builder instance and set available properties. */
    @Override
    public Project.Builder call() {
      logger.log(Level.DEBUG, "Scanning directory: {0}", base().toAbsolutePath());
      var builder = new Project.Builder();
      builder.setBase(base());
      scanName().ifPresent(builder::setName);
      scanVersion().ifPresent(builder::setVersion);
      return builder;
    }
  }

  /** A task representation, usually calling a tool by its name and passing arguments. */
  public interface Call {

    /** The name of the call. */
    String name();

    /** Arguments of the call. */
    List<String> args();

    /** Associated execution level. */
    default Level level() {
      return Level.ALL;
    }

    default String toMarkdown() {
      return "· `" + toString() + "`";
    }

    /** Create a named tool call. */
    static Call of(String name, String... args) {
      class Tool implements Call {
        @Override
        public String name() {
          return name;
        }

        @Override
        public List<String> args() {
          return List.of(args);
        }

        @Override
        public String toString() {
          return name + (args().isEmpty() ? "" : " " + String.join(" ", args()));
        }
      }
      return new Tool();
    }

    /** Create an active call providing an callable instance. */
    static Call of(String name, Callable<?> callable, String... code) {
      class Lambda implements Call, Callable<Object> {
        @Override
        public String name() {
          return name;
        }

        @Override
        public List<String> args() {
          return List.of(code);
        }

        @Override
        public Object call() throws Exception {
          return callable.call();
        }

        @Override
        public String toString() {
          return String.join(" ", args());
        }
      }
      return new Lambda();
    }
  }

  /** A composite task that is composed of a name and a list of nested {@link Call} instances. */
  public interface Plan extends Call {

    /** Nested calls of this plan. */
    List<Call> calls();

    /** Configuration level. */
    Level level();

    /** Indicates that all nested calls are independent of each other. */
    boolean parallel();

    /** Walk a tree of calls starting with this plan instance. */
    default int walk(BiConsumer<String, Call> consumer) {
      return walk(this, consumer);
    }

    /** Create named plan with an array of calls. */
    static Plan of(String name, Level level, boolean parallel, Call... calls) {
      class Record implements Plan {
        @Override
        public String name() {
          return name;
        }

        @Override
        public List<String> args() {
          return List.of();
        }

        @Override
        public Level level() {
          return level;
        }

        @Override
        public boolean parallel() {
          return parallel;
        }

        @Override
        public List<Call> calls() {
          return List.of(calls);
        }

        @Override
        public String toMarkdown() {
          return String.format(
              "» %s _(size=%d, level=%s, parallel=%s)_",
              name(), calls().size(), level(), parallel());
        }
      }
      return new Record();
    }

    /** Walk a tree of calls starting with the given call instance. */
    static int walk(Call call, BiConsumer<String, Call> consumer) {
      return walk(call, "", "  ", consumer);
    }

    /** Walk a tree of calls starting with the given call instance. */
    static int walk(Call call, String indent, String inc, BiConsumer<String, Call> consumer) {
      consumer.accept(indent, call);
      if (call instanceof Plan) {
        var plan = ((Plan) call);
        var count = 0;
        for (var child : plan.calls()) count += walk(child, indent + inc, inc, consumer);
        return count;
      }
      return 1;
    }
  }

  /** Folder configuration record. */
  /*record*/ public static final class Folder {
    private final Path out;
    private final Path lib;

    public Folder(Path base) {
      this(base.resolve(".bach"), base.resolve("lib"));
    }

    public Folder(Path out, Path lib) {
      this.out = out;
      this.lib = lib;
    }

    public Path out() {
      return out;
    }

    public Path lib() {
      return lib;
    }

    @Override
    public String toString() {
      return "Folder{out=" + out + ", lib=" + lib + "}";
    }
  }

  /** Project build plan computer. */
  public class Planner implements Callable<Plan> {

    private final Project project;
    private final Folder folder;

    /** Initialize this planner instance. */
    public Planner(Project project) {
      this(project, new Folder(project.base()));
    }

    /** Initialize this planner instance. */
    public Planner(Project project, Folder folder) {
      this.project = project;
      this.folder = folder;
      logger.log(Level.TRACE, "Initialized {0}", this);
    }

    /** Compute project build plan. */
    @Override
    public Plan call() {
      logger.log(Level.DEBUG, "Computing build plan for {0}", project);
      logger.log(Level.DEBUG, "Using folder configuration: {0}", folder);
      return Plan.of(
          "Build " + project.name() + " " + project.version(),
          Level.ALL,
          false,
          showSystemInformation(),
          createOutputDirectory());
    }

    /** Print system information. */
    public Call showSystemInformation() {
      return Plan.of(
          "Show System Information",
          Level.INFO,
          true,
          Call.of("javac", "--version"),
          Call.of("javadoc", "--version"),
          Call.of("jar", "--version"),
          Call.of("jdeps", "--version"));
    }

    /** Create output directory. */
    public Call createOutputDirectory() {
      return Call.of(
          "Create output directory",
          () -> Files.createDirectories(folder.out),
          "Files.createDirectories(" + Code.pathOf(folder.out) + ")");
    }
  }

  /** Java source code related helpers. */
  public static class Code {
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

  /** Execution configuration record. */
  /*record*/ public static final class Configuration {
    private final Project project;
    private final Plan plan;
    private final Set<Level> levels;
    private final boolean parallel;

    public Configuration(Project project, Plan plan) {
      this(project, plan, EnumSet.allOf(Level.class), true);
    }

    public Configuration(Project project, Plan plan, Set<Level> levels, boolean parallel) {
      this.project = project;
      this.plan = plan;
      this.levels = levels.isEmpty() ? EnumSet.noneOf(Level.class) : EnumSet.copyOf(levels);
      this.parallel = parallel;
    }

    public Project project() {
      return project;
    }

    public Plan plan() {
      return plan;
    }

    public Set<Level> levels() {
      return levels;
    }

    public boolean parallel() {
      return parallel;
    }
  }

  /** Plan executor. */
  public class Executor implements Callable<Summary> {

    private final Configuration configuration;
    private final Summary summary;

    public Executor(Configuration configuration) {
      this.configuration = configuration;
      this.summary = new Summary(configuration.project());
    }

    /** Compute summary by executing the configuration. */
    @Override
    public Summary call() {
      var start = Instant.now();
      walkAndExecute(configuration.plan());
      summary.duration = Duration.between(start, Instant.now());
      return summary;
    }

    /** Return true iff the passed call is to be executed. */
    protected boolean enabled(Call call) {
      if (call.level() == Level.ALL) return true; // ALL as in "always active"
      if (call.level() == Level.OFF) return false; // OFF as in "always off"
      return configuration.levels().contains(call.level());
    }

    /** Walk the call: either descend or execute it. */
    protected void walkAndExecute(Call call) {
      if (!enabled(call)) return;

      if (call instanceof Plan) {
        var plan = (Plan) call;
        var calls = plan.calls();
        var stream =
            configuration.parallel() && plan.parallel()
                ? calls.stream().parallel()
                : calls.stream();
        var start = Instant.now();
        stream.forEach(this::walkAndExecute);
        var duration = Duration.between(start, Instant.now()).toMillis();
        logger.log(Level.DEBUG, "{0} took {1} ms", plan.name(), duration);
        return;
      }

      try {
        logger.log(Level.DEBUG, "· {0}", call);
        summary.calls.add(call);
        var result = execute(call);
        if (result == null) return;
        logger.log(Level.TRACE, "Discarding result of {0}: {1}", call.name(), result);
      } catch (Exception e) {
        var message = "Computation failed: " + call;
        logger.log(Level.ERROR, message, e);
        throw new Error(message, e);
      }
    }

    /** Execute the given call (not a plan here). */
    protected Object execute(Call call) throws Exception {
      if (call instanceof Plan) throw new AssertionError("No plan expected here!");

      if (call instanceof Callable) return ((Callable<?>) call).call();

      var name = call.name();
      var tool = ToolProvider.findFirst(name);
      if (tool.isPresent()) {
        var out = new StringWriter();
        var err = new StringWriter();
        var array = call.args().toArray(String[]::new);
        var code = tool.get().run(new PrintWriter(out), new PrintWriter(err), array);
        out.toString().lines().forEach(line -> logger.log(Level.DEBUG, "{0}", line));
        err.toString().lines().forEach(line -> logger.log(Level.WARNING, "{0}", line));
        if (code != 0) {
          var message = name + " exited with code: " + code;
          logger.log(Level.ERROR, message);
          throw new Error(message, new RuntimeException(err.toString()));
        }
        return null;
      }
      throw new UnsupportedOperationException(name + " is not supported");
    }
  }

  /** Execution summary. */
  public static class Summary {
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
    public String toString() {
      return "Summary for " + project.name() + " -> " + calls.size() + " calls executed";
    }
  }
}
