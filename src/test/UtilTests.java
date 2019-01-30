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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class UtilTests {

  @Test
  void currentJavaHome() {
    assertTrue(Files.isDirectory(Util.currentJavaHome()));
  }

  @Test
  void tempDir() {
    assertEquals("tools", Util.path(Property.PATH_TOOLS).getFileName().toString());
  }

  @Test
  void isJavaFile() {
    assertFalse(Util.isJavaFile(Path.of("")));
    assertFalse(Util.isJavaFile(Path.of("a/b")));
    assertTrue(Util.isJavaFile(Path.of("src/test/UtilTests.java")));
    assertFalse(Util.isJavaFile(Path.of("src/test-resources/Util.isJavaFile.java")));
  }

  @Test
  void last() {
    assertEquals("", last(""));
    assertEquals("a", last("a"));
    assertEquals("b", last("a", "b"));
    assertEquals(File.separator, last("/"));
    assertEquals("\\", last("\\"));
    assertEquals("C:\\", last("C:\\"));
    assertEquals("a", last("/a"));
    assertEquals("a", last("\\a"));
    assertEquals("b", last("a/b"));
    assertEquals("b", last("a\\b"));
    assertEquals("b", last("/a/b"));
    assertEquals("b", last("\\a\\b"));
  }

  @Test
  void removeTreeForNonExistingPathFails() {
    var path = Path.of("does not exist");
    var e = assertThrows(UncheckedIOException.class, () -> Util.removeTree(path));
    assertEquals("removing tree failed: does not exist", e.getMessage());
  }

  @Test
  void removeTreeForEmptyDirectoryWorks() throws Exception {
    var temp = Files.createTempDirectory("bach-UtilTests.downloadUsingHttps-");
    assertTrue(Files.exists(temp));
    Util.removeTree(temp, __ -> true);
    assertFalse(Files.exists(temp));
  }

  private static String last(String first, String... more) {
    return Util.last(Path.of(first, more)).toString();
  }
}
