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

package de.sormuras.bach.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PathsTests {
  @Test
  void userHomeIsDirectory() {
    assertTrue(Files.isDirectory(Paths.USER_HOME));
  }

  @Test
  void listEmptyDirectoryYieldsAnEmptyListOfPaths(@TempDir Path temp) {
    assertEquals(List.of(), Paths.list(temp, "*"));
    assertEquals(List.of(), Paths.list(temp, Files::isRegularFile));
    assertEquals(List.of(), Paths.list(temp, Files::isDirectory));
  }

  @Test
  void normalize() {
    assertEquals("", Path.of("").normalize().toString());
    assertEquals("", Path.of(".").normalize().toString());
    assertEquals("", Path.of("a/..").normalize().toString());
    assertEquals("", Path.of("a/b/../../.").normalize().toString());
  }
}
