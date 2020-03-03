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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
/** Bach - Java Shell Builder. */
public class Bach {
  /** Version of the Java Shell Builder. */
  public static Version VERSION = Version.parse("11.0-ea");
  /** Default line printer instance delegates to {@link System#out}. */
  private static final Consumer<String> PRINTER = System.out::println;
  /** Default verbosity flag, including {@code -Debug} support. */
  private static final boolean VERBOSE =
      Boolean.getBoolean("verbose") // -D verbose=true
          || Boolean.getBoolean("ebug") // -Debug=true
          || "".equals(System.getProperty("ebug")); // -Debug
  /** Main entry-point. */
  public static void main(String... args) {
    Main.main(args);
  }
  /** Line-based message printing consumer. */
  private final Consumer<String> printer;
  /** Verbosity flag. */
  private final boolean verbose;
  /** Initialize this instance with the specified line printer and verbosity flag. */
  public Bach() {
    this(PRINTER, VERBOSE);
  }
  /** Initialize this instance with the specified line printer and verbosity flag. */
  public Bach(Consumer<String> printer, boolean verbose) {
    this.printer = printer;
    this.verbose = verbose;
    print(Level.TRACE, "Bach initialized");
  }
  /** Verbosity flag. */
  public boolean verbose() {
    return verbose;
  }
  /** Line printer. */
  Consumer<String> printer() {
    return printer;
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
  /** Build default project potentially modified by the passed project builder consumer. */
  public Summary build(Consumer<Project.Builder> projectBuilderConsumer) {
    return build(project(projectBuilderConsumer));
  }
  /** Build the specified project using the default build task generator. */
  public Summary build(Project project) {
    return build(project, new BuildTaskGenerator(project, verbose()));
  }
  /** Build the specified project using the given build task supplier. */
  Summary build(Project project, Supplier<Task> taskSupplier) {
    var start = Instant.now();
    print("Build %s", project.name());
    // if (verbose()) project.print(printer());
    var summary = new Summary(project);
    execute(taskSupplier.get(), summary);
    var markdown = summary.write();
    var duration =
        Duration.between(start, Instant.now())
            .truncatedTo(TimeUnit.MILLISECONDS.toChronoUnit())
            .toString()
            .substring(2)
            .replaceAll("(\\d[HMS])(?!$)", "$1 ")
            .toLowerCase();
    print("Build took %s -> %s", duration, markdown.toUri());
    return summary;
  }
  /** Run the given task and its attached child tasks. */
  void execute(Task task, Summary summary) {
    var markdown = task.toMarkdown();
    var children = task.children();
    print(Level.DEBUG, "%c %s", children.isEmpty() ? '*' : '+', markdown);
    summary.executionBegin(task);
    var result = task.execute(new ExecutionContext(this));
    if (verbose()) {
      result.out().lines().forEach(printer());
      result.err().lines().forEach(printer());
    }
    if (result.code() != 0) {
      result.err().lines().forEach(printer);
      summary.executionEnd(task, result);
      var message = markdown + ": non-zero result code: " + result.code();
      throw new RuntimeException(message);
    }
    if (!children.isEmpty()) {
      try {
        var tasks = task.parallel() ? children.parallelStream() : children.stream();
        tasks.forEach(child -> execute(child, summary));
      } catch (RuntimeException e) {
        summary.error().addSuppressed(e);
      }
      print(Level.DEBUG, "= %s", markdown);
    }
    summary.executionEnd(task, result);
  }
  /** Create new default project potentially modified by the passed project builder consumer. */
  Project project(Consumer<Project.Builder> projectBuilderConsumer) {
    // var projectBuilder = new ProjectScanner(paths).scan();
    var projectBuilder = Project.builder();
    projectBuilderConsumer.accept(projectBuilder);
    return projectBuilder.build();
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/api/Convention.java
  /**
   * Common conventions.
   *
   * @see <a href="https://github.com/sormuras/bach#common-conventions">Common Conventions</a>
   */
  interface Convention {
    /** Return name of main class of the specified module. */
    static Optional<String> mainClass(Path info, String module) {
      var main = Path.of(module.replace('.', '/'), "Main.java");
      var exists = Files.isRegularFile(info.resolveSibling(main));
      return exists ? Optional.of(module + '.' + "Main") : Optional.empty();
    }
    /** Extend the passed map of modules with missing JUnit test engine implementations. */
    static void amendJUnitTestEngines(Map<String, Version> modules) {
      var names = modules.keySet();
      if (names.contains("org.junit.jupiter") || names.contains("org.junit.jupiter.api"))
        modules.putIfAbsent("org.junit.jupiter.engine", null);
      if (names.contains("junit")) modules.putIfAbsent("org.junit.vintage.engine", null);
    }
    /** Extend the passed map of modules with the JUnit Platform Console module. */
    static void amendJUnitPlatformConsole(Map<String, Version> modules) {
      var names = modules.keySet();
      if (names.contains("org.junit.platform.console")) return;
      var triggers =
          Set.of("org.junit.jupiter.engine", "org.junit.vintage.engine", "org.junit.platform.engine");
      names.stream()
          .filter(triggers::contains)
          .findAny()
          .ifPresent(__ -> modules.put("org.junit.platform.console", null));
    }
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/api/Paths.java
  /** Common project-related paths. */
  public static final class Paths {
    private static final Path CLASSES = Path.of("classes");
    private static final Path MODULES = Path.of("modules");
    private static final Path SOURCES = Path.of("sources");
    private static final Path DOCUMENTATION = Path.of("documentation");
    private static final Path JAVADOC = DOCUMENTATION.resolve("javadoc");
    /** Create default instance for the specified base directory. */
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
    public Path lib() {
      return lib;
    }
    public Path out(String first, String... more) {
      var path = Path.of(first, more);
      return out.resolve(path);
    }
    public Path classes(String realm) {
      return out.resolve(CLASSES).resolve(realm);
    }
    public Path javadoc() {
      return out.resolve(JAVADOC);
    }
    public Path modules(String realm) {
      return out.resolve(MODULES).resolve(realm);
    }
    public Path sources(String realm) {
      return out.resolve(SOURCES).resolve(realm);
    }
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/api/Project.java
  /** Bach's project model. */
  public static final class Project {
    /** Return a mutable builder for creating a project instance. */
    public static Builder builder() {
      return new Builder();
    }
    private final String name;
    private final Version version;
    private final Structure structure;
    public Project(String name, Version version, Structure structure) {
      this.name = Objects.requireNonNull(name, "name");
      this.version = version;
      this.structure = Objects.requireNonNull(structure, "paths");
    }
    public String name() {
      return name;
    }
    public Version version() {
      return version;
    }
    public Structure structure() {
      return structure;
    }
    public Paths paths() {
      return structure().paths();
    }
    public String toNameAndVersion() {
      if (version == null) return name;
      return name + ' ' + version;
    }
    /** A mutable builder for a {@link Project}. */
    public static class Builder {
      private String name;
      private Version version;
      private Paths paths;
      private Builder() {
        name(null);
        version((Version) null);
        paths("");
      }
      public Project build() {
        var structure = new Structure(paths);
        return new Project(name, version, structure);
      }
      public Builder name(String name) {
        this.name = name;
        return this;
      }
      public Builder version(Version version) {
        this.version = version;
        return this;
      }
      public Builder version(String version) {
        return version(Version.parse(version));
      }
      public Builder paths(Paths paths) {
        this.paths = paths;
        return this;
      }
      public Builder paths(Path base) {
        return paths(Paths.of(base));
      }
      public Builder paths(String base) {
        return paths(Path.of(base));
      }
    }
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/api/Structure.java
  /** Project structure. */
  public static final class Structure {
    private final Paths paths;
    public Structure(Paths paths) {
      this.paths = paths;
    }
    public Paths paths() {
      return paths;
    }
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/execution/BuildTaskGenerator.java
  /** Generate default build task for a given project. */
  public static class BuildTaskGenerator implements Supplier<Task> {
    private final Project project;
    private final boolean verbose;
    public BuildTaskGenerator(Project project, boolean verbose) {
      this.project = project;
      this.verbose = verbose;
    }
    public Project project() {
      return project;
    }
    public boolean verbose() {
      return verbose;
    }
    @Override
    public Task get() {
      return sequence(
          "Build " + project().toNameAndVersion(),
          createDirectories(project.paths().out()),
          printVersionOfSelectedFoundationTools(),
          resolveMissingModules(),
          parallel(
              "Compile realms and generate API documentation",
              compileAllRealms(),
              compileApiDocumentation()),
          launchAllTests());
    }
    protected Task parallel(String title, Task... tasks) {
      return new Task(title, true, List.of(tasks));
    }
    protected Task sequence(String title, Task... tasks) {
      return new Task(title, false, List.of(tasks));
    }
    protected Task createDirectories(Path path) {
      return new Tasks.CreateDirectories(path);
    }
    protected Task printVersionOfSelectedFoundationTools() {
      return verbose()
          ? parallel(
              "Print version of various foundation tools"
              // tool("javac", "--version"),
              // tool("javadoc", "--version"),
              // tool("jar", "--version")
              )
          : sequence("Print version of javac");
    }
    protected Task resolveMissingModules() {
      return sequence("Resolve missing modules");
    }
    protected Task compileAllRealms() {
      return sequence("Compile all realms");
    }
    protected Task compileApiDocumentation() {
      return sequence("Compile API documentation");
    }
    protected Task launchAllTests() {
      return sequence("Launch all tests");
    }
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/execution/ExecutionContext.java
  /** Task execution context passed {@link Task#execute(ExecutionContext)}. */
  public static final class ExecutionContext {
    private final Bach bach;
    private final Instant start;
    private final StringWriter out;
    private final StringWriter err;
    public ExecutionContext(Bach bach) {
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
    public void print(Level level, String format, Object... args) {
      if (bach().verbose() || level.getSeverity() >= Level.INFO.getSeverity()) {
        var writer = level.getSeverity() <= Level.INFO.getSeverity() ? out : err;
        writer.write(String.format(format, args));
        writer.write(System.lineSeparator());
      }
    }
    /** Create result with code zero and empty output strings. */
    public ExecutionResult ok() {
      var duration = Duration.between(start(), Instant.now());
      return new ExecutionResult(0, duration, out.toString(), err.toString(), null);
    }
    /** Create result with error code one and append throwable's message to the error string. */
    public ExecutionResult failed(Throwable throwable) {
      var duration = Duration.between(start(), Instant.now());
      return new ExecutionResult(1, duration, out.toString(), err.toString(), throwable);
    }
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/execution/ExecutionResult.java
  /** Task execution result record returned by {@link Task#execute(ExecutionContext)}. */
  public static final class ExecutionResult {
    private final int code;
    private final Duration duration;
    private final String out;
    private final String err;
    private final Throwable throwable;
    public ExecutionResult(int code, Duration duration, String out, String err, Throwable throwable) {
      this.code = code;
      this.duration = duration;
      this.out = out;
      this.err = err;
      this.throwable = throwable;
    }
    public int code() {
      return code;
    }
    public Duration duration() {
      return duration;
    }
    public String out() {
      return out;
    }
    public String err() {
      return err;
    }
    public Throwable throwable() {
      return throwable;
    }
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/execution/Task.java
  /** Executable task definition. */
  public static class Task {
    private final String title;
    private final boolean parallel;
    private final List<Task> children;
    /** Initialize a task instance. */
    public Task(String title, boolean parallel, List<Task> children) {
      this.title = Objects.requireNonNull(title, "title");
      this.parallel = parallel;
      this.children = List.copyOf(children);
    }
    public String title() {
      return title;
    }
    public boolean parallel() {
      return parallel;
    }
    public List<Task> children() {
      return children;
    }
    /** Default computation called before executing child tasks. */
    public ExecutionResult execute(ExecutionContext execution) {
      return execution.ok();
    }
    /** Return markdown representation of this task instance. */
    public String toMarkdown() {
      return title();
    }
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/execution/Tasks.java
  /** Collection of tasks. */
  public interface Tasks {
    /** Creates a directory by creating all nonexistent parent directories first. */
    class CreateDirectories extends Task {
      private final Path path;
      public CreateDirectories(Path path) {
        super("Create directories " + path, false, List.of());
        this.path = path;
      }
      @Override
      public ExecutionResult execute(ExecutionContext context) {
        try {
          Files.createDirectories(path);
          return context.ok();
        } catch (Exception e) {
          return context.failed(e);
        }
      }
      @Override
      public String toMarkdown() {
        return "`Files.createDirectories(Path.of(" + path + "))`";
      }
    }
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/Main.java
  /** Bach's main program. */
  static class Main {
    public static void main(String... args) {
      System.out.println("Bach.java " + Bach.VERSION);
      new Bach().build(project -> project.name("project")).assertSuccessful();
    }
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/Summary.java
  /** Build summary. */
  public static final class Summary {
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
    public AssertionError error() {
      return error;
    }
    public void assertSuccessful() {
      var exceptions = error.getSuppressed();
      if (exceptions.length == 0) return;
      var one = exceptions[0]; // first suppressed exception
      if (exceptions.length == 1 && one instanceof RuntimeException) throw (RuntimeException) one;
      throw error;
    }
    public int countedChildlessTasks() {
      return details.size();
    }
    public int countedExecutionEvents() {
      return executions.size();
    }
    /** Task execution is about to begin callback. */
    void executionBegin(Task task) {
      if (task.children().isEmpty()) return;
      var format = "|   +|%6X|        | %s";
      var thread = Thread.currentThread().getId();
      var text = task.title();
      executions.add(String.format(format, thread, text));
    }
    /** Task execution ended callback. */
    void executionEnd(Task task, ExecutionResult result) {
      var format = "|%4c|%6X|%8d| %s";
      var children = task.children();
      var kind = children.isEmpty() ? result.code() == 0 ? ' ' : 'X' : '=';
      var thread = Thread.currentThread().getId();
      var millis = result.duration().toMillis();
      var title = children.isEmpty() ? "**" + task.title() + "**" : task.title();
      var row = String.format(format, kind, thread, millis, title);
      if (children.isEmpty()) {
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
      var md = new ArrayList<String>();
      var version = Optional.ofNullable(project.version());
      md.add("");
      md.add("## Project");
      md.add("- name: " + project.name());
      md.add("- version: " + version.map(Object::toString).orElse("_none_"));
      md.add("");
      md.add("```text");
      md.add(project.toString());
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
        var result = detail.result;
        md.add("### " + detail.caption);
        md.add(" - Command = " + detail.task.toMarkdown());
        md.add(" - Code = " + result.code());
        md.add(" - Duration = " + result.duration());
        md.add("");
        if (!result.out().isBlank()) {
          md.add("Normal (expected) output");
          md.add("```");
          md.add(result.out().strip());
          md.add("```");
        }
        if (!result.err().isBlank()) {
          md.add("Error output");
          md.add("```");
          md.add(result.err().strip());
          md.add("```");
        }
        if (result.throwable() != null) {
          var stackTrace = new StringWriter();
          result.throwable().printStackTrace(new PrintWriter(stackTrace));
          md.add("Throwable");
          md.add("```");
          stackTrace.toString().lines().forEach(md::add);
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
        var stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        md.add("### " + exception.getMessage());
        md.add("```text");
        stackTrace.toString().lines().forEach(md::add);
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
        Files.createDirectories(directory);
        return Files.write(directory.resolve("summary.md"), markdown);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    /** Task and its result tuple. */
    private static final class Detail {
      private final String caption;
      private final Task task;
      private final ExecutionResult result;
      private Detail(String caption, Task task, ExecutionResult result) {
        this.caption = caption;
        this.task = task;
        this.result = result;
      }
    }
  }
}
