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

package de.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.base.FileSystem;

class PathsTests {

  @Test
  void isEmptyWalkThrough(@TempDir Path directory) throws Exception {
    assertTrue(Paths.isEmpty(directory));
    var file = Files.createFile(directory.resolve("regular.file"));
    assertTrue(Paths.isEmpty(file));
    Files.writeString(file, "Hello world!");
    assertFalse(Paths.isEmpty(file));
    assertFalse(Paths.isEmpty(directory));
    Files.delete(file);
    assertTrue(Paths.isEmpty(directory));
    var subdirectory = Files.createDirectory(directory.resolve("subdirectory"));
    assertFalse(Paths.isEmpty(directory));
    Files.delete(subdirectory);
    assertTrue(Paths.isEmpty(directory));
  }

  @Test
  void isEmptyFailsForNotReadablePath(@TempDir Path temp) throws Exception {
    var sub = Files.createDirectory(temp.resolve("sub"));
    assertTrue(Paths.isEmpty(sub));
    FileSystem.chmod(sub, false, false, false);
    try {
      assertThrows(UncheckedIOException.class, () -> Paths.isEmpty(sub));
    } finally {
      FileSystem.chmod(sub, true, true, true);
    }
  }
}
