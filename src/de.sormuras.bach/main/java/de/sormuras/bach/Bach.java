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

import de.sormuras.bach.task.CheckProjectState;
import de.sormuras.bach.task.CreateDirectories;
import de.sormuras.bach.task.PrintModules;
import de.sormuras.bach.task.PrintProject;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

/** Bach - Java Shell Builder. */
public class Bach {

  /** Version of the Java Shell Builder. */
  public static final Version VERSION = Version.parse("11.0-ea");

  /** Main entry-point. */
  public static void main(String... args) {
    Main.main(args);
  }

  /** Line-based message printing consumer. */
  private final Consumer<String> printer;

  /** Verbosity flag. */
  private final boolean verbose;

  /** Dry-run flag. */
  private final boolean dryRun;

  /** Directory for storing generated assets into. */
  private final Workspace workspace;

  /** Initialize this instance with default values. */
  public Bach() {
    this(
        System.out::println,
        Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug")),
        Boolean.getBoolean("ry-run") || "".equals(System.getProperty("ry-run")),
        Workspace.of());
  }

  /** Initialize this instance with the specified line printer and verbosity flag. */
  public Bach(Consumer<String> printer, boolean verbose, boolean dryRun, Workspace workspace) {
    this.printer = Objects.requireNonNull(printer, "printer");
    this.verbose = verbose;
    this.dryRun = dryRun;
    this.workspace = workspace;
    print(Level.DEBUG, "%s initialized", this);
    print(Level.TRACE, "\tverbose=%s", verbose);
    print(Level.TRACE, "\tdry-run=%s", dryRun);
    print(Level.TRACE, "\tWorkspace");
    print(Level.TRACE, "\t\tbase='%s' -> %s", workspace.base(), workspace.base().toUri());
    print(Level.TRACE, "\t\tworkspace=%s", workspace.workspace());
  }

  public boolean isVerbose() {
    return verbose;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  public Workspace getWorkspace() {
    return workspace;
  }

  /** Print a message at information level. */
  public String print(String format, Object... args) {
    return print(Level.INFO, format, args);
  }

  /** Print a message at specified level. */
  public String print(Level level, String format, Object... args) {
    var message = String.format(format, args);
    if (verbose || level.getSeverity() >= Level.INFO.getSeverity()) printer.accept(message);
    return message;
  }

  /** Build the given project using default settings. */
  public void build(Project project) {
    var tasks = new ArrayList<Task>();
    if (verbose) tasks.add(new PrintProject(printer, project));
    tasks.add(new CheckProjectState(project));
    tasks.add(new CreateDirectories(workspace.workspace()));
    // javac/jar main realm | javadoc
    // jlink    | javac/jar test realm
    // jpackage | junit test realm
    tasks.add(new PrintModules(printer, project));
    build(project, new Task("Build project " + project.toNameAndVersion(), false, tasks));
  }

  /** Build the given project using the provided task. */
  public void build(Project project, Task build) {
    print(Level.DEBUG, "Build project: %s", project.toNameAndVersion());
    print(Level.DEBUG, "Build task: %s", build.name());
    if (dryRun) return;
    execute(build);
  }

  /** Execute the given task recursively. */
  public void execute(Task task) {
    var executor = new Task.Executor(this);
    print(Level.TRACE, "");
    print(Level.TRACE, "Execute task: " + task.name());
    var summary = executor.execute(task).assertSuccessful();
    if (verbose) {
      print("");
      print("Task Execution Overview");
      print("|    |Thread|Duration| Task");
      summary.getOverviewLines().forEach(this::print);
    }
    var count = summary.getTaskCount();
    var duration = summary.getDuration().toMillis();
    print(Level.DEBUG, "Execution of %d tasks took %d ms", count, duration);
  }

  @Override
  public String toString() {
    return "Bach.java " + VERSION;
  }
}
