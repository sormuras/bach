/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

import de.sormuras.bach.Bach;
import de.sormuras.bach.Task;
import de.sormuras.bach.util.Maven;
import de.sormuras.bach.util.Paths;
import java.nio.file.Files;

public class CompileTask implements Task {

  @Override
  public void execute(Bach bach) {
    var log = bach.getLog();
    var project = bach.getProject();
    var lib = project.folder().lib();
    if (Files.isDirectory(lib)) {
      log.debug("Library %s contains", lib.toAbsolutePath());
      Paths.walk(lib, name -> log.debug("  %s", name));
    }
    for (var realm : project.structure().realms()) {
      var units = project.units(realm);
      if (units.isEmpty()) continue;
      log.debug("Compiling %d %s unit(s): %s", units.size(), realm.name(), units);
      new Hydra(bach, realm).compile(units);
      new Jigsaw(bach, realm).compile(units);
      var scribe = new Maven.Scribe(project);
      scribe.generateMavenInstallScript(units);
      if (realm.isDeployRealm()) scribe.generateMavenDeployScript(units);
    }
  }
}
