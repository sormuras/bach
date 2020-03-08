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

import de.sormuras.bach.api.Project;
import de.sormuras.bach.execution.ExecutionResult;
import de.sormuras.bach.execution.Snippet;
import de.sormuras.bach.execution.Task;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/** Build summary. */
public /*static*/ final class Summary {

  private final Project project;
  private final List<String> program;
  private final Deque<String> executions = new ConcurrentLinkedDeque<>();
  private final Deque<Detail> details = new ConcurrentLinkedDeque<>();
  private final Deque<Throwable> suppressed = new ConcurrentLinkedDeque<>();

  public Summary(Project project, Task root) {
    this.project = project;
    this.program = Snippet.program(root);
  }

  public boolean aborted() {
    return !suppressed.isEmpty();
  }

  public void addSuppressed(Throwable throwable) {
    suppressed.add(throwable);
  }

  public void assertSuccessful() {
    if (suppressed.isEmpty()) return;
    var message = new StringJoiner("\n");
    message.add(String.format("collected %d suppressed throwable(s)", suppressed.size()));
    message.add(String.join("\n", toMarkdown()));
    var error = new AssertionError(message.toString());
    suppressed.forEach(error::addSuppressed);
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
    executions.add(String.format(format, thread, task.title()));
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
    md.addAll(buildProgram());
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
    md.addAll(project.toStrings());
    md.add("```");
    return md;
  }

  private List<String> buildProgram() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Build Program");
    md.add("");
    md.add("```text");
    md.addAll(program);
    md.add("```");
    return md;
  }

  private List<String> taskExecutionOverview() {
    if (executions.isEmpty()) return List.of();
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
      md.add(" - Task Title = " + detail.task.title());
      md.add(" - Exit Code = " + result.code());
      md.add(" - Duration = " + result.duration());
      md.add("");
      md.add("```");
      md.addAll(detail.task.toSnippet().lines());
      md.add("```");
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
    // if (value.endsWith("\\")) return value + ' '; // make trailing backslash visible
    if (!"line.separator".equals(systemPropertyKey)) return value;
    var build = new StringBuilder();
    for (char c : value.toCharArray()) {
      build.append("0x").append(Integer.toHexString(c).toUpperCase());
    }
    return build.toString();
  }

  public Path write() {
    @SuppressWarnings("SpellCheckingInspection")
    var pattern = "yyyyMMddHHmmss";
    var formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneOffset.UTC);
    var timestamp = formatter.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
    var markdown = toMarkdown();
    try {
      var out = project.paths().out();
      var summaries = out.resolve("summaries");
      Files.createDirectories(summaries);
      Files.write(project.paths().out("Build.java"), program);
      Files.write(summaries.resolve("summary-" + timestamp + ".md"), markdown);
      return Files.write(out.resolve("summary.md"), markdown);
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
