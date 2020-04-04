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
import java.util.function.Consumer;

/** Print information about the compiled modules. */
public /*static*/ class PrintModules extends Task {

  private final Consumer<String> printer;
  private final Project project;

  public PrintModules(Consumer<String> printer, Project project) {
    super("Print modules");
    this.printer = printer;
    this.project = project;
  }

  @Override
  public void execute(Execution context) {
    var realm = project.structure().realms().get(0);
    for (var unit : realm.units()) {
      var jar = context.bach.getWorkspace().jarFilePath(project, realm, unit);
      printer.accept("Unit " + unit.descriptor().toNameAndVersion());
      printer.accept("jar=" + jar);
    }
  }
}
