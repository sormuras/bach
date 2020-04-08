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
import java.lang.System.Logger.Level;

/** Print information about all compiled modules. */
public /*static*/ class PrintModules extends Task {

  private final Project project;

  public PrintModules(Project project) {
    super("Print modules");
    this.project = project;
  }

  @Override
  public void execute(Execution execution) {
    var realm = project.structure().realms().get(0);
    for (var unit : realm.units()) {
      var jar = execution.getBach().getWorkspace().jarFilePath(project, realm, unit);
      var nameAndVersion = unit.descriptor().toNameAndVersion();
      execution.print(Level.INFO, "Unit " + nameAndVersion, "\t-> " + jar.toUri());
    }
  }
}
