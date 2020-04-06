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

import java.io.PrintWriter;
import java.io.StringWriter;
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
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** An executable task definition. */
public /*static*/ class Task {

  private final String name;
  private final boolean parallel;
  private final List<Task> subs;

  public Task(String name) {
    this(name, false, List.of());
  }

  public Task(String name, boolean parallel, List<Task> subs) {
    this.name = Objects.requireNonNullElse(name, getClass().getSimpleName());
    this.parallel = parallel;
    this.subs = subs;
  }

  public String name() {
    return name;
  }

  public void execute(Execution execution) throws Exception {}

  public static Task parallel(String name, Task... tasks) {
    return new Task(name, true, List.of(tasks));
  }

  public static Task sequence(String name, Task... tasks) {
    return new Task(name, false, List.of(tasks));
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

    @Override
    public boolean isPrintable(Level level) {
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
      private final Execution execution;
      private final String caption;
      private final Duration duration;

      private Detail(Execution execution, String caption, Duration duration) {
        this.execution = execution;
        this.caption = caption;
        this.duration = duration;
      }
    }

    private final Bach bach;
    private final Project project;
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
      var subs = task.subs;
      var flat = subs.isEmpty(); // i.e. no sub tasks
      var printer = bach.getPrinter();
      printer.print(Level.TRACE, String.format("%s%c %s", indent, flat ? '*' : '+', name));
      executionBegin(task);
      var execution = new Execution(bach, indent);
      try {
        task.execute(execution);
        if (!flat) {
          var stream = task.parallel ? subs.parallelStream() : subs.stream();
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
      if (task.subs.isEmpty()) return;
      var format = "|   +|%6X|        | %s";
      var thread = Thread.currentThread().getId();
      overview.add(String.format(format, thread, task.name));
    }

    private void executionEnd(Task task, Execution execution) {
      var format = "|%4c|%6X|%8d| %s";
      var flat = task.subs.isEmpty();
      var kind = flat ? ' ' : '=';
      var thread = Thread.currentThread().getId();
      var duration = Duration.between(execution.start, Instant.now());
      var line = String.format(format, kind, thread, duration.toMillis(), task.name);
      if (flat) {
        var caption = "task-execution-details-" + execution.hash;
        overview.add(line + " [...](#" + caption + ")");
        executions.add(new Detail(execution, caption, duration));
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

      int getTaskCount() {
        return executions.size();
      }

      List<String> toMarkdown() {
        var md = new ArrayList<String>();
        md.add("# Summary");
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
        md.add("```text");
        md.addAll(project.toStrings());
        md.add("```");
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
          md.add(" - Execution Start Instant = " + result.execution.start);
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
