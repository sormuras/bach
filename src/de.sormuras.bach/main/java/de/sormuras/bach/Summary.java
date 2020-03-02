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

import de.sormuras.bach.execution.ExecutionResult;
import de.sormuras.bach.execution.Task;
import de.sormuras.bach.model.Project;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;

/** Build summary. */
public /*static*/ final class Summary {

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
