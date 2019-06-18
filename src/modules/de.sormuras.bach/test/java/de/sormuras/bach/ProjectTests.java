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

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectTests {

  @Test
  void defaultPropertiesInAnEmptyDirectory(@TempDir Path empty) {
    var project = Project.of(empty);
    var name = empty.getFileName().toString();
    var version = "1.0.0-SNAPSHOT";
    assertEquals(name, project.name);
    assertEquals(version, project.version);
    assertEquals("*", project.get(Project.Property.MODULES));
    assertEquals(Path.of("bin"), project.path(Project.Property.PATH_BIN));
    assertEquals(Path.of("lib"), project.path(Project.Property.PATH_LIB));
    assertEquals(Path.of("src"), project.path(Project.Property.PATH_SRC));
    assertEquals(name + ' ' + version, project.toString());
  }
}
