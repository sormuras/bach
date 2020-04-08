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

package de.sormuras.bach;

import de.sormuras.bach.project.structure.Directory;
import de.sormuras.bach.task.RunTool;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/** An executable task definition. */
public /*static*/ class Task {

  private final String name;
  private final boolean composite;
  private final boolean parallel;
  private final List<Task> subs;

  public Task(String name) {
    this(name, false, false, List.of());
  }

  public Task(String name, boolean parallel, List<Task> subs) {
    this(name, true, parallel, subs);
  }

  public Task(String name, boolean composite, boolean parallel, List<Task> subs) {
    this.name = Objects.requireNonNullElse(name, getClass().getSimpleName());
    this.composite = composite;
    this.parallel = parallel;
    this.subs = subs;
  }

  public String name() {
    return name;
  }

  public boolean composite() {
    return composite;
  }

  public boolean parallel() {
    return parallel;
  }

  public List<Task> subs() {
    return subs;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Task.class.getSimpleName() + "[", "]")
        .add("name='" + name + "'")
        .add("composite=" + composite)
        .add("parallel=" + parallel)
        .add("subs=" + subs)
        .toString();
  }

  public boolean leaf() {
    return !composite;
  }

  public void execute(Execution execution) throws Exception {}

  public int size() {
    var counter = new AtomicInteger();
    walk(task -> counter.incrementAndGet());
    return counter.get();
  }

  void walk(Consumer<Task> consumer) {
    consumer.accept(this);
    for (var sub : subs) sub.walk(consumer);
  }

  public static Task parallel(String name, Task... tasks) {
    return new Task(name, true, List.of(tasks));
  }

  public static Task sequence(String name, Task... tasks) {
    return new Task(name, false, List.of(tasks));
  }

  /** Create new tool-running task for the given tool instance. */
  public static Task run(Tool tool) {
    return run(tool.name(), tool.args().toArray(String[]::new));
  }

  /** Create new tool-running task for the name and arguments. */
  public static Task run(String name, String... args) {
    return run(ToolProvider.findFirst(name).orElseThrow(), args);
  }

  /** Create new tool-running task for the given tool and its options. */
  public static Task run(ToolProvider provider, String... args) {
    return new RunTool(provider, args);
  }

  public static class Execution implements Printer {
    private final Bach bach;
    private final String indent;
    private final String hash = Integer.toHexString(System.identityHashCode(this));
    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();
    private final Instant start = Instant.now();

    private Execution(Bach bach, String indent) {
      this.bach = bach;
      this.indent = indent;
    }

    public Bach getBach() {
      return bach;
    }

    public Writer getOut() {
      return out;
    }

    public Writer getErr() {
      return err;
    }

    @Override
    public boolean printable(Level level) {
      return true;
    }

    @Override
    public void print(Level level, String message) {
      bach.getPrinter().print(level, message.lines().map(line -> indent + line));
      var writer = level.getSeverity() <= Level.INFO.getSeverity() ? out : err;
      writer.write(message);
      writer.write(System.lineSeparator());
    }
  }

  static class Executor {

    private static final class Detail {
      private final Task task;
      private final Execution execution;
      private final String caption;
      private final Duration duration;

      private Detail(Task task, Execution execution, String caption, Duration duration) {
        this.task = task;
        this.execution = execution;
        this.caption = caption;
        this.duration = duration;
      }
    }

    private final Bach bach;
    private final Project project;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final Deque<String> overview = new ConcurrentLinkedDeque<>();
    private final Deque<Detail> executions = new ConcurrentLinkedDeque<>();

    Executor(Bach bach, Project project) {
      this.bach = bach;
      this.project = project;
    }

    Summary execute(Task task) {
      var start = Instant.now();
      var throwable = execute(0, task);
      return new Summary(task, Duration.between(start, Instant.now()), throwable);
    }

    private Throwable execute(int depth, Task task) {
      var indent = "\t".repeat(depth);
      var name = task.name;
      var printer = bach.getPrinter();
      printer.print(Level.TRACE, String.format("%s%c %s", indent, task.leaf() ? '*' : '+', name));
      executionBegin(task);
      var execution = new Execution(bach, indent);
      try {
        task.execute(execution);
        if (task.composite()) {
          var stream = task.parallel ? task.subs.parallelStream() : task.subs.stream();
          var errors = stream.map(sub -> execute(depth + 1, sub)).filter(Objects::nonNull);
          var error = errors.findFirst();
          if (error.isPresent()) return error.get();
          printer.print(Level.TRACE, indent + "= " + name);
        }
        executionEnd(task, execution);
      } catch (Exception exception) {
        printer.print(Level.ERROR, "Task execution failed: " + exception);
        return exception;
      }
      return null;
    }

    private void executionBegin(Task task) {
      if (task.leaf()) return;
      var format = "|   +|%6X|        | %s";
      var thread = Thread.currentThread().getId();
      overview.add(String.format(format, thread, task.name));
    }

    private void executionEnd(Task task, Execution execution) {
      counter.incrementAndGet();
      var format = "|%4c|%6X|%8d| %s";
      var kind = task.leaf() ? ' ' : '=';
      var thread = Thread.currentThread().getId();
      var duration = Duration.between(execution.start, Instant.now());
      var line = String.format(format, kind, thread, duration.toMillis(), task.name);
      if (task.leaf()) {
        var caption = "task-execution-details-" + execution.hash;
        overview.add(line + " [...](#" + caption + ")");
        executions.add(new Detail(task, execution, caption, duration));
        return;
      }
      overview.add(line);
    }

    class Summary {

      private final Task task;
      private final Duration duration;
      private final Throwable exception;

      Summary(Task task, Duration duration, Throwable exception) {
        this.task = task;
        this.duration = duration;
        this.exception = exception;
      }

      void assertSuccessful() {
        if (exception == null) return;
        var message = task.name + " (" + task.getClass().getSimpleName() + ") failed";
        throw new AssertionError(message, exception);
      }

      String toDurationString() {
        return duration
            .truncatedTo(TimeUnit.MILLISECONDS.toChronoUnit())
            .toString()
            .substring(2)
            .replaceAll("(\\d[HMS])(?!$)", "$1 ")
            .toLowerCase();
      }

      int getTaskCounter() {
        return counter.get();
      }

      List<String> toMarkdown() {
        var md = new ArrayList<String>();
        md.add("# Summary");
        md.add("- Java " + Runtime.version());
        md.add("- " + System.getProperty("os.name"));
        md.add("- Executed task `" + task.name + "`");
        md.add("- Build took " + toDurationString());
        md.addAll(exceptionDetails());
        md.addAll(projectDescription());
        md.addAll(taskExecutionOverview());
        md.addAll(taskExecutionDetails());
        md.addAll(systemProperties());
        return md;
      }

      List<String> exceptionDetails() {
        if (exception == null) return List.of();
        var md = new ArrayList<String>();
        md.add("");
        md.add("## Exception Details");
        var lines = String.valueOf(exception.getMessage()).lines().collect(Collectors.toList());
        md.add("### " + (lines.isEmpty() ? exception.getClass() : lines.get(0)));
        if (lines.size() > 1) md.addAll(lines);
        var stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        md.add("```text");
        stackTrace.toString().lines().forEach(md::add);
        md.add("```");
        return md;
      }

      List<String> projectDescription() {
        if (project == null) return List.of();
        var md = new ArrayList<String>();
        md.add("");
        md.add("## Project");
        md.add("- `name` = `\"" + project.name() + "\"`");
        md.add("- `version` = `" + project.version() + "`");
        md.add("- `uri` = " + project.information().uri());
        md.add("- `description` = " + project.information().description());
        md.add("");
        md.add("|Realm|Unit|Directories|");
        md.add("|-----|----|-----------|");
        var structure = project.structure();
        for (var realm : structure.realms()) {
          for (var unit : realm.units()) {
            var directories =
                unit.directories().stream()
                    .map(Directory::toMarkdown)
                    .collect(Collectors.joining("<br>"));
            var realmName = realm.name();
            var unitName = unit.name();
            md.add(
                String.format(
                    "| %s | %s | %s",
                    realmName.equals(structure.mainRealm()) ? "**" + realmName + "**" : realmName,
                    unitName.equals(realm.mainUnit()) ? "**" + unitName + "**" : unitName,
                    directories));
          }
        }
        return md;
      }

      List<String> taskExecutionOverview() {
        if (overview.isEmpty()) return List.of();
        var md = new ArrayList<String>();
        md.add("");
        md.add("## Task Execution Overview");
        md.add("|    |Thread|Duration|Caption");
        md.add("|----|-----:|-------:|-------");
        md.addAll(overview);
        return md;
      }

      List<String> taskExecutionDetails() {
        if (executions.isEmpty()) return List.of();
        var md = new ArrayList<String>();
        md.add("");
        md.add("## Task Execution Details");
        md.add("");
        for (var result : executions) {
          md.add("### " + result.caption);
          md.add(" - **" + result.task.name() + "**");
          md.add(" - Started = " + result.execution.start);
          md.add(" - Duration = " + result.duration);
          md.add("");
          var out = result.execution.out.toString();
          if (!out.isBlank()) {
            md.add("Normal (expected) output");
            md.add("```");
            md.add(out.strip());
            md.add("```");
          }
          var err = result.execution.err.toString();
          if (!err.isBlank()) {
            md.add("Error output");
            md.add("```");
            md.add(err.strip());
            md.add("```");
          }
        }
        return md;
      }

      List<String> systemProperties() {
        var md = new ArrayList<String>();
        md.add("");
        md.add("## System Properties");
        System.getProperties().stringPropertyNames().stream()
            .sorted()
            .forEach(key -> md.add(String.format("- `%s`: `%s`", key, systemProperty(key))));
        return md;
      }

      String systemProperty(String systemPropertyKey) {
        var value = System.getProperty(systemPropertyKey);
        // if (value.endsWith("\\")) return value + ' '; // make trailing backslash visible
        if (!"line.separator".equals(systemPropertyKey)) return value;
        var builder = new StringBuilder();
        for (char c : value.toCharArray()) {
          builder.append("0x").append(Integer.toHexString(c).toUpperCase());
        }
        return builder.toString();
      }

      void write(String prefix) {
        @SuppressWarnings("SpellCheckingInspection")
        var pattern = "yyyyMMddHHmmss";
        var formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneOffset.UTC);
        var timestamp = formatter.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        var workspace = bach.getWorkspace();
        var summary = workspace.workspace("summary", prefix + "-" + timestamp + ".md");
        var markdown = toMarkdown();
        try {
          Files.createDirectories(summary.getParent());
          Files.write(summary, markdown);
          Files.write(workspace.workspace("summary.md"), markdown); // replace existing
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
  }
}
