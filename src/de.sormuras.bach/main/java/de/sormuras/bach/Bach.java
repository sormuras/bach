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

import de.sormuras.bach.execution.ExecutionContext;
import de.sormuras.bach.execution.Task;
import de.sormuras.bach.model.Project;
import de.sormuras.bach.model.ProjectBuilder;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.util.function.Consumer;

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
  private Consumer<String> printer() {
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
  public Summary build(Consumer<ProjectBuilder> projectBuilderConsumer) {
    return build(project(projectBuilderConsumer));
  }

  /** Build the specified project. */
  public Summary build(Project project) {
    return new Summary(project);
  }

  /** Run the given task and its attached child tasks. */
  void execute(Task task, Summary summary) {
    var markdown = task.toMarkdown();
    print(Level.DEBUG, markdown);

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

    var children = task.children();
    if (!children.isEmpty()) {
      try {
        var tasks = task.parallel() ? children.parallelStream() : children.stream();
        tasks.forEach(child -> execute(child, summary));
      } catch (RuntimeException e) {
        summary.error().addSuppressed(e);
      }
    }

    summary.executionEnd(task, result);
  }

  /** Create new default project potentially modified by the passed project builder consumer. */
  Project project(Consumer<ProjectBuilder> projectBuilderConsumer) {
    // var projectBuilder = new ProjectScanner(paths).scan();
    var projectBuilder = new ProjectBuilder();
    projectBuilderConsumer.accept(projectBuilder);
    return projectBuilder.build();
  }
}
