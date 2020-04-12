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
import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.util.spi.ToolProvider;

/** Print information about all compiled modules. */
public /*static*/ class PrintModules extends Task {

  private final Project project;

  public PrintModules(Project project) {
    super("Print modules");
    this.project = project;
  }

  @Override
  public void execute(Execution execution) throws Exception {
    var workspace = execution.getBach().getWorkspace();
    var realm = project.structure().toMainRealm().orElseThrow();
    for (var unit : realm.units()) {
      var jar = workspace.module(realm.name(), unit.name(), project.toModuleVersion(unit));
      execution.print(Level.INFO, "file: " + jar.getFileName(), "size: " + Files.size(jar));
      var out = new PrintWriter(execution.getOut());
      var err = new PrintWriter(execution.getErr());
      ToolProvider.findFirst("jar")
          .orElseThrow()
          .run(out, err, "--describe-module", "--file", jar.toString());
    }
  }
}
