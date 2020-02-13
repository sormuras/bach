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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Java Shell Builder.
 *
 * <p>Requires JDK 11 or later.
 */
public class Bach {

  /** Version of the Java Shell Builder. */
  private static final Version VERSION = Version.parse("11.0-ea");

  /** Default logger instance. */
  private static final Logger LOGGER = System.getLogger("Bach.java");

  /** Bach.java's main program entry-point. */
  public static void main(String... args) {
    try {
      new Bach().new Main(args).run();
    } catch (Throwable throwable) {
      throw new Error("Main program of Bach.java failed", throwable);
    }
  }

  /** Logger instance. */
  private final Logger logger;

  /** Line-based message printing consumer. */
  private final Consumer<String> printer;

  /** Verbosity flag. */
  private final boolean verbose;

  /** Initialize this instance with default values. */
  public Bach() {
    this(LOGGER, System.out::println, Boolean.getBoolean("verbose"));
  }

  /** Initialize this instance with the specified arguments. */
  public Bach(Logger logger, Consumer<String> printer, boolean verbose) {
    this.logger = logger;
    this.printer = printer;
    this.verbose = verbose;
    logger.log(Level.TRACE, "Initialized Bach.java " + VERSION);
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

  /** Build the specified project using the default build task factory. */
  public Build.Summary build(Project project) {
    var factory = new Build.Factory(project);
    return Build.build(this, project, factory::newBuildTask);
  }

  /** Project model. */
  public static final class Project implements Util.Printable {

    /** Base path of the project. */
    private final Paths paths;

    /** Project descriptor. */
    private final ModuleDescriptor descriptor;

    /** Initialize this project model. */
    public Project(Paths paths, ModuleDescriptor descriptor) {
      this.paths = paths;
      this.descriptor = descriptor;
    }

    /** Project paths. */
    public Paths paths() {
      return paths;
    }

    /** Project model descriptor. */
    public ModuleDescriptor descriptor() {
      return descriptor;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", Project.class.getSimpleName() + "[", "]")
          .add("paths=" + paths)
          .add("descriptor=" + descriptor)
          .toString();
    }

    /** Common project-related paths. */
    public static final class Paths implements Util.Printable {

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

    /** Project model builder. */
    public static class Builder {

      /** Paths of the project. */
      private Paths paths;

      /** Project model descriptor builder. */
      private final ModuleDescriptor.Builder descriptor;

      /** Initialize this project model builder with the given name. */
      Builder(String name) {
        this.paths = Paths.of(Path.of(""));
        var synthetic = Set.of(ModuleDescriptor.Modifier.SYNTHETIC);
        this.descriptor = ModuleDescriptor.newModule(name, synthetic);
      }

      /** Create new project model instance based on this builder's components. */
      public Project build() {
        return new Project(paths, descriptor.build());
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

      /** Declare a dependence on the specified module and version. */
      public Builder requires(String module, String version) {
        var synthetic = Set.of(ModuleDescriptor.Requires.Modifier.SYNTHETIC);
        descriptor.requires(synthetic, module, Version.parse(version));
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
        var builder = new Project.Builder(scanName().orElse("nameless"));
        builder.paths(paths);
        return builder;
      }

      /** Return name of the project. */
      public Optional<String> scanName() {
        return Optional.of(base().toAbsolutePath().getFileName()).map(Object::toString);
      }
    }
  }

  /** Namespace for build-related types. */
  public interface Build {

    /** Build the specified project using the given root task supplier. */
    static Summary build(Bach bach, Project project, Supplier<Task> task) {
      var start = Instant.now();
      bach.logger.log(Level.DEBUG, "Build {0}", project);
      bach.printer.accept("Build " + project.descriptor().toNameAndVersion());

      var summary = new Summary(project);
      execute(bach, task.get(), summary);

      var markdown = summary.write();
      var duration =
          Duration.between(start, Instant.now())
              .toString()
              .substring(2)
              .replaceAll("(\\d[HMS])(?!$)", "$1 ")
              .toLowerCase();

      bach.printer.accept("Summary written to " + markdown.toUri());
      bach.printer.accept("Build took " + duration);
      return summary;
    }

    /** Run the given task and its attached child tasks. */
    static void execute(Bach bach, Task task, Summary summary) {
      var markdown = task.toMarkdown();
      bach.logger.log(Level.DEBUG, markdown);
      if (bach.verbose) bach.printer.accept(markdown);

      summary.executionBegin(task);
      var result = task.call();
      if (bach.verbose) {
        result.out.lines().forEach(bach.printer);
        result.err.lines().forEach(bach.printer);
      }
      if (result.code != 0) {
        result.err.lines().forEach(bach.printer);
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

      public Factory(Project project) {
        this.project = project;
      }

      public Build.Task newBuildTask() {
        return sequence(
            "Build project " + project.descriptor().name(),
            new Task.CreateDirectories(project.paths.out),
            parallel(
                "Print version of various foundation tools",
                tool("javac", "--version"),
                tool("javadoc", "--version"),
                tool("jar", "--version")));
      }
    }

    /** An executable task and a potentially non-empty list of sub-tasks. */
    class Task implements Callable<Result> {

      /** Tool-running task. */
      public static final class RunTool extends Task {

        private final ToolProvider tool;
        private final String[] args;

        public RunTool(String caption, ToolProvider tool, String... args) {
          super(caption, false, List.of());
          this.tool = tool;
          this.args = args;
        }

        @Override
        public Result call() {
          var out = new StringWriter();
          var err = new StringWriter();
          var now = Instant.now();
          var code = tool.run(new PrintWriter(out), new PrintWriter(err), args);
          return new Result(now, code, out.toString(), err.toString());
        }

        @Override
        public String toMarkdown() {
          var arguments = args.length == 0 ? "" : ' ' + String.join(" ", args);
          return '`' + tool.name() + arguments + '`';
        }
      }

      /**
       * Task delegating to {@link Files#createDirectories(Path,
       * java.nio.file.attribute.FileAttribute[])}.
       */
      public static final class CreateDirectories extends Task {

        private final Path path;

        public CreateDirectories(Path path) {
          super("Create directories " + path, false, List.of());
          this.path = path;
        }

        @Override
        public Result call() {
          try {
            Files.createDirectories(path);
            return Result.ok();
          } catch (IOException e) {
            return Result.failed(e);
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
      @Override
      public Result call() {
        return Result.ok();
      }

      /** Return markdown representation of this task instance. */
      public String toMarkdown() {
        return caption;
      }
    }

    /** Execution result record. */
    final class Result {

      /** Create result with code zero and empty output strings. */
      public static Result ok() {
        return new Result(Instant.now(), 0, "", "");
      }

      /** Create result with error code one and use throwable's message as the error string. */
      public static Result failed(Throwable throwable) {
        return new Result(Instant.now(), 1, "", throwable.toString());
      }

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
        private final Task task;
        private final Result result;

        public Detail(Task task, Result result) {
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
        if (result.out.isBlank() && result.err.isBlank()) {
          executions.add(row);
          return;
        }
        var hash = Integer.toHexString(System.identityHashCode(task));
        executions.add(row + " [...](#details-" + hash + ")");
        details.add(new Detail(task, result));
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
        md.add("- version: " + descriptor.version());
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
          var task = detail.task;
          var result = detail.result;
          var hash = Integer.toHexString(System.identityHashCode(task));
          md.add("### <a name='details-" + hash + "'/> " + task.caption);
          md.add(" - Command = " + task.toMarkdown());
          md.add(" - Start Instant = " + result.start);
          md.add(" - Exit Code = " + result.code);
          md.add("");
          if (!detail.result.out.isBlank()) {
            md.add("Normal (expected) output");
            md.add("```text");
            md.add(result.out.strip());
            md.add("```");
          }
          if (!detail.result.err.isBlank()) {
            md.add("Error output");
            md.add("```text");
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
        return describe(Strings.readString(info));
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

      /** Compute module's source path. */
      static String moduleSourcePath(Path path, String module) {
        var directory = path.endsWith("module-info.java") ? path.getParent() : path;
        var names = new ArrayList<String>();
        directory.forEach(element -> names.add(element.toString()));
        int frequency = Collections.frequency(names, module);
        if (frequency == 0) {
          return directory.toString();
        }
        if (frequency == 1) {
          if (directory.endsWith(module)) {
            return Optional.ofNullable(directory.getParent()).map(Path::toString).orElse(".");
          }
          var elements = names.stream().map(name -> name.equals(module) ? "*" : name);
          return elements.collect(Collectors.joining(File.separator));
        }
        throw new IllegalArgumentException("Ambiguous module source path: " + path);
      }

      /** Return modular origin of the given object. */
      static String origin(Object object) {
        var module = object.getClass().getModule();
        if (module.isNamed()) {
          return module.getDescriptor().toNameAndVersion();
        }
        try {
          var uri = object.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
          return Path.of(uri).getFileName().toString();
        } catch (NullPointerException | URISyntaxException ignore) {
          return module.toString();
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
              for (var element : collection) {
                if (element instanceof Printable) {
                  ((Printable) element).print(context.nested(value), indent + "  ");
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
      /** Read all content from a file into a string. */
      static String readString(Path path) {
        try {
          return Files.readString(path);
        } catch (IOException e) {
          throw new UncheckedIOException("Read all content from file failed: " + path, e);
        }
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
    void run() {
      logger.log(Level.DEBUG, "Run main operation(s): " + operations);
      if (operations.isEmpty()) return;
      var operation = operations.removeFirst();
      switch (operation) {
        case "build":
          build();
          return;
        case "help":
          help();
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
      printer.accept("Bach.java " + VERSION + " running on Java " + Runtime.version());
      printer.accept("F1 F1 F1");
    }

    /** Print version. */
    public void version() {
      printer.accept("" + VERSION);
    }
  }
}
