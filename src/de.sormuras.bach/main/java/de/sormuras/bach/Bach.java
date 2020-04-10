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

import de.sormuras.bach.task.ValidateProject;
import de.sormuras.bach.task.CreateDirectories;
import de.sormuras.bach.task.PrintModules;
import de.sormuras.bach.task.PrintProject;
import de.sormuras.bach.task.ValidateWorkspace;
import de.sormuras.bach.tool.Tool;
import de.sormuras.bach.util.Strings;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.util.ArrayList;
import java.util.Objects;

/** Bach - Java Shell Builder. */
public class Bach {

  /** Version of the Java Shell Builder. */
  public static final Version VERSION = Version.parse("11.0-ea");

  /** Main entry-point. */
  public static void main(String... args) {
    Main.main(args);
  }

  /** Line-based message printer. */
  private final Printer printer;

  /** Directory for storing generated assets into. */
  private final Workspace workspace;

  /** Initialize this instance with default values. */
  public Bach() {
    this(Printer.ofSystem(), Workspace.of());
  }

  /** Initialize this instance with the specified line printer and verbosity flag. */
  public Bach(Printer printer, Workspace workspace) {
    this.printer = Objects.requireNonNull(printer, "printer");
    this.workspace = workspace;
    printer.print(
        Level.DEBUG,
        this + " initialized",
        "\tprinter=" + printer,
        "\tWorkspace",
        "\t\tbase='" + workspace.base() + "' -> " + workspace.base().toUri(),
        "\t\tworkspace=" + workspace.workspace());
  }

  public Printer getPrinter() {
    return printer;
  }

  public Workspace getWorkspace() {
    return workspace;
  }

  /** Build the given project using default settings. */
  public void build(Project project) {
    var tasks = new ArrayList<Task>();
    tasks.add(
        Task.parallel(
            "Versions",
            Task.run(Tool.of("javac", "--version")),
            Task.run("jar", "--version"),
            Task.run("javadoc", "--version")));
    tasks.add(new ValidateWorkspace());
    tasks.add(new PrintProject(project));
    tasks.add(new ValidateProject(project));
    tasks.add(new CreateDirectories(workspace.workspace()));
    // javac/jar main realm | javadoc
    // jlink    | javac/jar test realm
    // jpackage | junit test realm
    tasks.add(new PrintModules(project));
    var task = new Task("Build project " + project.toNameAndVersion(), false, tasks);
    build(project, task);
  }

  void build(Project project, Task task) {
    var summary = execute(new Task.Executor(this, project), task);
    summary.write("build");
    summary.assertSuccessful();
    printer.print(Level.INFO, "Build took " + summary.toDurationString());
  }

  public void execute(Task task) {
    execute(new Task.Executor(this, null), task).assertSuccessful();
  }

  private Task.Executor.Summary execute(Task.Executor executor, Task task) {
    var size = task.size();
    printer.print(Level.DEBUG, "Execute " + size + " tasks");
    var summary = executor.execute(task);
    printer.print(Level.DEBUG, "Executed " + summary.getTaskCounter() + " of " + size + " tasks");
    var exception = Strings.text(summary.exceptionDetails());
    if (!exception.isEmpty()) printer.print(Level.ERROR, exception);
    return summary;
  }

  @Override
  public String toString() {
    return "Bach.java " + VERSION;
  }
}
