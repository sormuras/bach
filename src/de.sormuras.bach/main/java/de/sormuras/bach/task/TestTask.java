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

import de.sormuras.bach.Bach;
import de.sormuras.bach.Task;

public class TestTask implements Task {

  @Override
  public void execute(Bach bach) throws Exception {
    var log = bach.getLog();
    var project = bach.getProject();
    for (var realm : project.structure().realms()) {
      if (!realm.isTestRealm()) continue;
      var units = project.units(realm);
      if (units.isEmpty()) continue;
      log.debug("Testing %d %s unit(s): %s", units.size(), realm.name(), units);
      new Tester(bach, realm).test(units);
    }
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      System.gc(); // Release JAR files held ModuleLayer instance
      Thread.sleep(256);
    }
  }
}
