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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.sormuras.bach.api.Projects;
import java.io.PrintWriter;
import org.junit.jupiter.api.Test;

class TestLauncherTests {

  @Test
  void checkToolTester() {
    var project = Projects.newProjectWithAllBellsAndWhistles();
    var test = project.structure().realms().get(1);
    var unit = test.unit("beta").orElseThrow();
    var tool = new TestLauncher.ToolTester(project, test, unit);
    var discard = new PrintWriter(PrintWriter.nullWriter());
    var noop = new NoopToolProvider(0, false);
    assertThrows(RuntimeException.class, () -> tool.run(discard, discard));
    assertDoesNotThrow(() -> tool.run(noop, discard, discard));
  }

  @Test
  void checkJUnitTester() {
    var project = Projects.newProjectWithAllBellsAndWhistles();
    var test = project.structure().realms().get(1);
    var unit = test.unit("beta").orElseThrow();
    var tool = new TestLauncher.JUnitTester(project, test, unit);
    assertThrows(RuntimeException.class, () -> tool.run(System.out, System.err));
  }
}
