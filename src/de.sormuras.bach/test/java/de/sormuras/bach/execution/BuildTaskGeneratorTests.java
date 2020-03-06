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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.api.Projects;
import org.junit.jupiter.api.Test;
import test.base.jdk.Compilation;

class BuildTaskGeneratorTests {

  @Test
  void checkBuildTaskGenerationForMultiModuleProjectWithTests() {
    var project = Projects.newProjectWithAllBellsAndWhistles();
    var generator = new BuildTaskGenerator(project, true);
    assertSame(project, generator.project());
    assertTrue(generator.verbose());
    var program = Snippet.program(generator.get());
    try {
      assertDoesNotThrow(() -> Compilation.compile(program));
    } catch (AssertionError e) {
      program.forEach(System.err::println);
      throw e;
    }
  }
}
