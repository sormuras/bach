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

import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
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

  public static class Execution {
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

    public void print(Level level, String message) {
      var LS = System.lineSeparator();
      bach.print(level, message.lines().map(line -> indent + line).collect(Collectors.joining(LS)));
      var writer = level.getSeverity() <= Level.INFO.getSeverity() ? out : err;
      var enable = writer == err || level == Level.INFO || bach.isVerbose();
      if (enable) writer.write(message + LS);
    }
  }

  static class Executor {

    private final Bach bach;
    private final Deque<String> overview = new ConcurrentLinkedDeque<>();
    private final Deque<Execution> executions = new ConcurrentLinkedDeque<>();

    Executor(Bach bach) {
      this.bach = bach;
    }

    Summary execute(Task root) {
      var start = Instant.now();
      var throwable = execute(0, root);
      return new Summary(root.name, Duration.between(start, Instant.now()), throwable);
    }

    private Throwable execute(int depth, Task task) {
      var indent = "\t".repeat(depth);
      var name = task.name;
      var subs = task.subs;
      var flat = subs.isEmpty(); // i.e. no sub tasks
      bach.print(Level.TRACE, String.format("%s%c %s", indent, flat ? '*' : '+', name));
      executionBegin(task);
      var execution = new Execution(bach, indent);
      try {
        task.execute(execution);
        if (!flat) {
          var stream = task.parallel ? subs.parallelStream() : subs.stream();
          var errors = stream.map(sub -> execute(depth + 1, sub)).filter(Objects::nonNull);
          var error = errors.findFirst();
          if (error.isPresent()) return error.get();
          bach.print(Level.TRACE, indent + "= " + name);
        }
        executionEnd(task, execution);
      } catch (Exception exception) {
        bach.print(Level.ERROR, "Task execution failed: " + exception);
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
      var millis = Duration.between(execution.start, Instant.now()).toMillis();
      var line = String.format(format, kind, thread, millis, task.name);
      if (flat) {
        overview.add(line + " [...](#task-execution-details-" + execution.hash + ")");
        executions.add(execution);
        return;
      }
      overview.add(line);
    }

    class Summary {

      private final String title;
      private final Duration duration;
      private final Throwable throwable;

      Summary(String title, Duration duration, Throwable throwable) {
        this.title = title;
        this.duration = duration;
        this.throwable = throwable;
      }

      Summary assertSuccessful() {
        if (throwable == null) return this;
        throw new AssertionError(title + " failed", throwable);
      }

      Duration getDuration() {
        return duration;
      }

      Deque<String> getOverviewLines() {
        return overview;
      }

      int getTaskCount() {
        return executions.size();
      }
    }
  }
}
