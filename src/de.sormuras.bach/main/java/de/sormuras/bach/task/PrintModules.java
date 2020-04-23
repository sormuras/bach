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
import de.sormuras.bach.util.Paths;
import de.sormuras.bach.util.Strings;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

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
      if (!execution.printable(Level.DEBUG)) continue;
      var bytes = Files.readAllBytes(jar);
      execution.print(Level.DEBUG, " md5: " + Paths.digest("Md5", bytes));
      execution.print(Level.DEBUG, "sha1: " + Paths.digest("sha1", bytes));
      var out = new StringWriter();
      var err = new StringWriter();
      var tool = ToolProvider.findFirst("jar").orElseThrow();
      var args = new String[] {"--describe-module", "--file", jar.toString()};
      var code = tool.run(new PrintWriter(out), new PrintWriter(err), args);
      var lines = out.toString().strip().lines().skip(1).collect(Collectors.toList());
      execution.print(Level.DEBUG, Strings.textIndent("\t", lines));
      if (code != 0) execution.print(Level.ERROR, err.toString());
    }
  }
}
