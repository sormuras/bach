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

package de.sormuras.bach.execution;

import de.sormuras.bach.api.Project;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/** Generate default build task for a given project. */
public /*static*/ class BuildTaskGenerator implements Supplier<Task> {

  private final Project project;
  private final boolean verbose;

  public BuildTaskGenerator(Project project, boolean verbose) {
    this.project = project;
    this.verbose = verbose;
  }

  public Project project() {
    return project;
  }

  public boolean verbose() {
    return verbose;
  }

  @Override
  public Task get() {
    return sequence(
        "Build " + project().toNameAndVersion(),
        createDirectories(project.paths().out()),
        printVersionOfSelectedFoundationTools(),
        resolveMissingModules(),
        parallel(
            "Compile realms and generate API documentation",
            compileAllRealms(),
            compileApiDocumentation()),
        launchAllTests());
  }

  protected Task parallel(String title, Task... tasks) {
    return new Task(title, true, List.of(tasks));
  }

  protected Task sequence(String title, Task... tasks) {
    return new Task(title, false, List.of(tasks));
  }

  protected Task createDirectories(Path path) {
    return new Tasks.CreateDirectories(path);
  }

  protected Task printVersionOfSelectedFoundationTools() {
    return verbose()
        ? parallel(
            "Print version of various foundation tools"
            // tool("javac", "--version"),
            // tool("javadoc", "--version"),
            // tool("jar", "--version")
            )
        : sequence("Print version of javac");
  }

  protected Task resolveMissingModules() {
    return sequence("Resolve missing modules");
  }

  protected Task compileAllRealms() {
    return sequence("Compile all realms");
  }

  protected Task compileApiDocumentation() {
    return sequence("Compile API documentation");
  }

  protected Task launchAllTests() {
    return sequence("Launch all tests");
  }
}
