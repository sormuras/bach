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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        "\tPrinter",
        "\t\tverbose=" + printer.isVerbose(),
        "\t\tlevels="
            + Stream.of(Level.values())
                .map(level -> level + "=" + printer.isEnabled(level))
                .collect(Collectors.joining(", ")),
        "\tWorkspace",
        String.format("\t\tbase='%s' -> %s", workspace.base(), workspace.base().toUri()),
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
    tasks.add(new PrintProject(project));
    tasks.add(new CheckProjectState(project));
    tasks.add(new CreateDirectories(workspace.workspace()));
    // javac/jar main realm | javadoc
    // jlink    | javac/jar test realm
    // jpackage | junit test realm
    tasks.add(new PrintModules(project));
    execute(new Task("Build project " + project.toNameAndVersion(), false, tasks));
  }

  /** Execute the given task recursively. */
  public void execute(Task task) {
    var executor = new Task.Executor(this);
    printer.print(Level.TRACE, "", "Execute task: " + task.name());
    var summary = executor.execute(task).assertSuccessful();
    printer.print(
        Level.TRACE,
        "",
        "Task Execution Overview",
        "|    |Thread|Duration| Task",
        String.join(System.lineSeparator(), summary.getOverviewLines()));
    var count = summary.getTaskCount();
    var duration = summary.getDuration().toMillis();
    printer.print(Level.INFO, String.format("Execution of %d tasks took %d ms", count, duration));
  }

  @Override
  public String toString() {
    return "Bach.java " + VERSION;
  }
}
