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
import de.sormuras.bach.execution.BuildTaskGenerator;
import de.sormuras.bach.execution.ExecutionContext;
import de.sormuras.bach.execution.Task;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Bach - Java Shell Builder. */
public class Bach {

  /** Version of the Java Shell Builder. */
  public static final Version VERSION = Version.parse("11.0-ea");

  /** Main entry-point. */
  public static void main(String... args) {
    Main.main(args);
  }

  /** Shared heavyweight singletons. */
  private final Atomics atomics;

  /** Line-based message printing consumer. */
  private final Consumer<String> printer;

  /** Verbosity flag. */
  private final boolean debug;

  /** Dry-run flag. */
  private final boolean dryRun;

  /** Initialize this instance with default values. */
  public Bach() {
    this(
        new Atomics(),
        System.out::println,
        Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug")),
        Boolean.getBoolean("ry-run") || "".equals(System.getProperty("ry-run")));
  }

  /** Initialize this instance with the specified line printer and verbosity flag. */
  public Bach(Atomics atomics, Consumer<String> printer, boolean debug, boolean dryRun) {
    this.atomics = Objects.requireNonNull(atomics, "atomics");
    this.printer = Objects.requireNonNull(printer, "printer");
    this.debug = debug;
    this.dryRun = dryRun;
    print(Level.TRACE, "Bach initialized");
  }

  /** Shared heavyweight singletons. */
  public Atomics atomics() {
    return atomics;
  }

  /** Verbosity flag. */
  public boolean debug() {
    return debug;
  }

  /** Print a message at information level. */
  public String print(String format, Object... args) {
    return print(Level.INFO, format, args);
  }

  /** Print a message at specified level. */
  public String print(Level level, String format, Object... args) {
    var message = String.format(format, args);
    if (debug || level.getSeverity() >= Level.INFO.getSeverity()) printer.accept(message);
    return message;
  }

  /** Build default project potentially modified by the passed project builder consumer. */
  public Summary build(Consumer<Project.Builder> projectBuilderConsumer) {
    var base = Path.of("");
    var builder = Project.scanner(base).scan();
    projectBuilderConsumer.accept(builder);
    var project = builder.build();
    return build(project);
  }

  /** Build the specified project using the default build task generator. */
  public Summary build(Project project) {
    return build(project, new BuildTaskGenerator(project, debug));
  }

  /** Build the specified project using the given build task supplier. */
  Summary build(Project project, Supplier<Task> taskSupplier) {
    var start = Instant.now();
    var build = dryRun ? "Dry-run" : "Build";
    print("%s %s", build, project.toNameAndVersion());
    var task = taskSupplier.get();
    var summary = new Summary(project, task);
    // if (verbose || dryRun) summary.program().forEach(printer);
    execute(task, summary);
    var markdown = summary.write();
    var duration =
        Duration.between(start, Instant.now())
            .truncatedTo(TimeUnit.MILLISECONDS.toChronoUnit())
            .toString()
            .substring(2)
            .replaceAll("(\\d[HMS])(?!$)", "$1 ")
            .toLowerCase();
    print("%s took %s -> %s", build, duration, markdown.toUri());
    return summary;
  }

  /** Run the given task and its attached child tasks. */
  void execute(Task task, Summary summary) {
    execute(0, task, summary);
  }

  private void execute(int depth, Task task, Summary summary) {
    if (dryRun || summary.aborted()) return;

    var indent = "\t".repeat(depth);
    var title = task.title();
    var children = task.children();
    print(Level.DEBUG, "%s%c %s", indent, children.isEmpty() ? '*' : '+', title);

    summary.executionBegin(task);
    var result = task.execute(new ExecutionContext(this, summary));
    if (debug) {
      result.out().lines().forEach(printer);
      result.err().lines().forEach(printer);
    }
    if (result.code() != 0) {
      result.err().lines().forEach(printer);
      summary.executionEnd(task, result);
      var message = title + ": non-zero result code: " + result.code();
      throw new RuntimeException(message);
    }

    if (!children.isEmpty()) {
      try {
        var tasks = task.parallel() ? children.parallelStream() : children.stream();
        tasks.forEach(child -> execute(depth + 1, child, summary));
      } catch (RuntimeException e) {
        summary.addSuppressed(e);
      }
      print(Level.DEBUG, "%s= %s", indent, title);
    }

    summary.executionEnd(task, result);
  }
}
