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

package de.sormuras.bach.task;

import de.sormuras.bach.Project;
import de.sormuras.bach.Task;
import java.nio.file.Files;
import java.util.function.Consumer;

/** Print the strings-representation of project. */
public /*static*/ class PrintProject extends Task {

  private final Consumer<String> printer;
  private final Project project;

  public PrintProject(Consumer<String> printer, Project project) {
    super("Print project");
    this.printer = printer;
    this.project = project;
  }

  @Override
  public void execute(Execution context) {
    project.toStrings().forEach(printer);
  }
}
