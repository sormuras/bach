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

package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectTests {

  @Test
  void defaultPropertiesInAnEmptyDirectory(@TempDir Path empty) {
    var project = Project.of(empty, empty);
    var name = empty.getFileName().toString();
    var version = "1.0.0-SNAPSHOT";
    assertEquals(name, project.name);
    assertEquals(version, project.version);
    assertEquals("{}", project.properties.toString());
    assertEquals("*", project.get(Project.Property.MODULES));
    assertEquals(empty.resolve("bin"), project.bin);
    assertEquals(empty.resolve("lib"),  project.lib);
    assertEquals(empty.resolve("src"), project.src);
    assertEquals(name + ' ' + version, project.toString());

    assertLinesMatch(List.of(), project.modules("realm"));
    assertTrue(project.modulePath("realm", "phase").isEmpty());
    var e = assertThrows(IllegalArgumentException.class, () -> project.modulePath("r", "p", "r"));
    assertEquals("Cyclic realm dependency detected: r", e.getMessage());
  }

  @Test
  void modulesParsedFromUserDefinedModulesString() {
    assertLinesMatch(List.of("a", "b", "c"), Project.modules("realm", " a  ,b, c ", Path.of("")));
  }

  @Test
  void modulesFailsForScanningAnIllegalRootDirectory() {
    var readme = Path.of("README.md");
    assertTrue(Files.isRegularFile(readme), "Not a regular file?! " + readme);
    var e = assertThrows(Error.class, () -> Project.modules("realm", "*", readme));
    assertEquals(
        "Scanning directory for modules failed: java.nio.file.NotDirectoryException: README.md",
        e.getMessage());
  }
}
