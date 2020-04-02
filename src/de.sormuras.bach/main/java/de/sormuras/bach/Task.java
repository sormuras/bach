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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/** Executable task definition. */
public /*static*/ class Task {

  private final String name;
  private final boolean parallel;
  private final List<Task> subs;

  public Task() {
    this(null, false, List.of());
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

  public static class Execution {
    private final String hash = Integer.toHexString(System.identityHashCode(this));
    public final StringWriter out = new StringWriter();
    public final StringWriter err = new StringWriter();
    private final Instant start = Instant.now();
  }

  static class Executor {

    private final Bach bach;
    private final Deque<String> overview = new ConcurrentLinkedDeque<>();
    private final Deque<Execution> executions = new ConcurrentLinkedDeque<>();
    private final Deque<Throwable> suppressed = new ConcurrentLinkedDeque<>();

    Executor(Bach bach) {
      this.bach = bach;
    }

    Summary execute(Task root) {
      var execution = execute(0, root);
      return new Summary(Duration.between(execution.start, Instant.now()));
    }

    private Execution execute(int depth, Task task) {
      var indent = "\t".repeat(depth);
      var name = task.name;
      var subs = task.subs;
      var flat = subs.isEmpty(); // i.e. no sub tasks
      bach.print(Level.TRACE, "%s%c %s", indent, flat ? '*' : '+', name);
      executionBegin(task);
      var execution = new Execution();
      try {
        task.execute(execution);
        if (!flat) {
          var stream = task.parallel ? subs.parallelStream() : subs.stream();
          stream.forEach(sub -> execute(depth + 1, sub));
          bach.print(Level.TRACE, "%s= %s", indent, name);
        }
        executionEnd(task, execution);
      } catch (Exception e) {
        suppressed.add(e);
      }
      return execution;
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
      var name = flat ? task.name : "**" + task.name + "**";
      var line = String.format(format, kind, thread, millis, name);
      if (flat) {
        overview.add(line + " [...](#task-execution-details-" + execution.hash + ")");
        executions.add(execution);
        return;
      }
      overview.add(line);
    }

    class Summary {

      private final Duration duration;

      Summary(Duration duration) {
        this.duration = duration;
      }

      Summary assertSuccessful() {
        if (suppressed.isEmpty()) return this;
        var message = new StringJoiner("\n");
        message.add(String.format("collected %d suppressed throwable(s)", suppressed.size()));
        message.add(String.join("\n", toExceptionDetails()));
        var error = new AssertionError(message.toString());
        suppressed.forEach(error::addSuppressed);
        throw error;
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

      private List<String> toExceptionDetails() {
        if (suppressed.isEmpty()) return List.of();
        var md = new ArrayList<String>();
        md.add("");
        md.add("## Exception Details");
        md.add("");
        md.add("- Caught " + suppressed.size() + " throwable(s).");
        md.add("");
        for (var throwable : suppressed) {
          var lines = throwable.getMessage().lines().collect(Collectors.toList());
          md.add("### " + (lines.isEmpty() ? throwable.getClass() : lines.get(0)));
          if (lines.size() > 1) md.addAll(lines);
          var stackTrace = new StringWriter();
          throwable.printStackTrace(new PrintWriter(stackTrace));
          md.add("```text");
          stackTrace.toString().lines().forEach(md::add);
          md.add("```");
        }
        return md;
      }
    }
  }
}
