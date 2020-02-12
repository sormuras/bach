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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;

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
    var bach = new Bach();
    var main = bach.new Main(args);
    var code = main.run();
    if (code != 0) throw new Error("Non-zero exit code: " + code);
  }

  /** Create new project model builder instance for the given name. */
  public static Project.Builder newProject(String name) {
    return new Project.Builder(name);
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

  /** Build the specified project. */
  public Build.Summary build(Project project) {
    logger.log(Level.DEBUG, "Build {0}", project);
    var summary = new Build.Summary(project);
    var task = new Build.Factory(project).newBuildTask();
    execute(task, summary);
    var markdown = summary.write();
    printer.accept("Summary written to " + markdown.toUri());
    return summary;
  }

  /** Run the given task and its attached child tasks. */
  void execute(Build.Task task, Build listener) {
    logger.log(Level.DEBUG, task.caption);
    if (verbose) printer.accept(task.toMarkdown());

    listener.executionBegin(task);
    var result = task.call();
    if (verbose) {
      result.out.lines().forEach(printer);
      result.err.lines().forEach(printer);
    }
    if (result.code != 0) throw new RuntimeException("Non-zero result code: " + result.code);

    var children = task.children;
    if (!children.isEmpty()) {
      var tasks = task.parallel ? children.parallelStream() : children.stream();
      tasks.forEach(child -> execute(child, listener));
    }

    listener.executionEnd(task, result);
  }

  /** Bach.java's main program class. */
  private class Main {

    private final Deque<String> operations;

    /** Initialize this instance with the given command line arguments. */
    private Main(String... arguments) {
      this.operations = new ArrayDeque<>(List.of(arguments));
    }

    /** Run main operation. */
    int run() {
      logger.log(Level.DEBUG, "Call main operation(s): " + operations);
      if (operations.isEmpty()) return 0;
      var operation = operations.removeFirst();
      switch (operation) {
        case "build":
          return build();
        case "help":
          return help();
        case "version":
          return version();
        default:
          throw new UnsupportedOperationException(operation);
      }
    }

    public int build() {
      var project = newProject("xxx").build();
      Bach.this.build(project);
      return 0;
    }

    /** Print help screen. */
    public int help() {
      printer.accept("Bach.java " + VERSION + " running on Java " + Runtime.version());
      printer.accept("F1 F1 F1");
      return 0;
    }

    /** Print version. */
    public int version() {
      printer.accept("" + VERSION);
      return 0;
    }
  }

  /** Project model. */
  public static final class Project {

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
    public static final class Paths {

      public static Paths of(Path base) {
        return new Paths(base, base.resolve(".bach"));
      }

      private final Path base;
      private final Path out;

      public Paths(Path base, Path out) {
        this.base = base;
        this.out = out;
      }

      public Path base() {
        return base;
      }

      public Path out() {
        return out;
      }

      @Override
      public String toString() {
        return new StringJoiner(", ", Paths.class.getSimpleName() + "[", "]")
            .add("base='" + base + "'")
            .add("out='" + out + "'")
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
  }

  /** Namespace for build-related types. */
  public interface Build {

    /** Create new tool-running task for the given tool name. */
    static Task newToolTask(String name, String... args) {
      var caption = String.format("Run `%s` with %d argument(s)", name, args.length);
      var provider = ToolProvider.findFirst(name);
      var tool = provider.orElseThrow(() -> new NoSuchElementException("Tool not found: " + name));
      return new Task.ToolTask(caption, tool, args);
    }

    /** Task execution is about to begin callback. */
    default void executionBegin(Task task) {}

    /** Task execution ended callback. */
    default void executionEnd(Task task, Result result) {}

    /** Build task factory. */
    class Factory {

      private final Project project;

      public Factory(Project project) {
        this.project = project;
      }

      public Build.Task newBuildTask() {
        var caption = "Build project " + project.descriptor().name();
        return new Build.Task(
            caption,
            false,
            List.of(
                new Task.CreateDirectories(project.paths.out),
                new Task(
                    "Print version of various foundation tools",
                    true,
                    List.of(
                        newToolTask("javac", "--version"),
                        newToolTask("javadoc", "--version"),
                        newToolTask("jar", "--version") //
                        ))));
      }
    }

    /** An executable task and a potentially non-empty list of sub-tasks. */
    class Task implements Callable<Result> {

      /** Tool-running task. */
      public static final class ToolTask extends Task {

        private final ToolProvider tool;
        private final String[] args;

        public ToolTask(String caption, ToolProvider tool, String... args) {
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
        return Result.UNDEFINED;
      }

      /** Return markdown representation of this task instance. */
      public String toMarkdown() {
        return caption;
      }
    }

    /** Execution result record. */
    final class Result {

      /** No-operation result constant. */
      private static final Result UNDEFINED = new Result(Instant.ofEpochMilli(0), 0, "", "");

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

      public Summary(Project project) {
        this.project = project;
      }

      @Override
      public void executionBegin(Task task) {
        if (task.children.isEmpty()) return;
        var format = "+|%04X|      ms| %s";
        var thread = Thread.currentThread().getId();
        var text = task.caption;
        executions.add(String.format(format, thread, text));
      }

      @Override
      public void executionEnd(Task task, Result result) {
        var format = "%c|%04X|%5d ms| %s";
        var kind = task.children.isEmpty() ? '*' : '=';
        var thread = Thread.currentThread().getId();
        var millis = Duration.between(result.start, Instant.now()).toMillis();
        var text = task.caption;
        var row = String.format(format, kind, thread, millis, text);
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
        md.addAll(systemProperties());
        return md;
      }

      private List<String> projectDescription() {
        var md = new ArrayList<String>();
        md.add("");
        md.add("## Project");
        md.add("`" + project + "`");
        return md;
      }

      private List<String> taskExecutionOverview() {
        var md = new ArrayList<String>();
        md.add("");
        md.add("## Task Execution Overview");
        md.add("|Kind|Thread|Duration|Message|");
        md.add("|----|------|--------|-------|");
        md.addAll(executions);
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
  }
}
