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

package de.sormuras.bach.api;

import static de.sormuras.bach.Assertions.assertToStringEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.API;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DirectoryTests {

  @Test
  void empty() {
    var empty = API.emptyDirectory();
    assertEquals(Path.of("empty"), empty.path());
    assertEquals(0, empty.release());
    assertTrue(empty.toString().contains(Directory.class.getSimpleName()));
  }

  @Test
  void ofPath() {
    var directory = Directory.of(Path.of("java-123"));
    assertEquals(Path.of("java-123"), directory.path());
    assertEquals(123, directory.release());
  }

  @Test
  void listOfNonexistentPathReturnsAnEmptyList() {
    var directories = Directory.listOf(Path.of("nonexistent"));
    assertTrue(directories.isEmpty());
  }

  @Test
  void listOfPathToRegularFileFails(@TempDir Path temp) throws Exception {
    var file = Files.createFile(temp.resolve("file"));
    assertThrows(UncheckedIOException.class, () -> Directory.listOf(file));
  }

  @Test
  void listOfPathWithNoSubdirectoryReturnsAnEmptyList(@TempDir Path temp) {
    var directories = Directory.listOf(temp);
    assertTrue(directories.isEmpty());
  }

  @Test
  void listOfPathWithSingleSubdirectoryReturnsIt(@TempDir Path temp) throws Exception {
    var java = Files.createDirectories(temp.resolve("java"));
    var directories = Directory.listOf(temp);
    assertToStringEquals(new Directory(java, 0), directories.get(0));
  }

  @Test
  void listOfPathWithMultipleSubdirectoriesReturnsThem(@TempDir Path temp) throws Exception {
    var java8 = new Directory(Files.createDirectories(temp.resolve("java-8")), 8);
    var java9 = new Directory(Files.createDirectories(temp.resolve("java-9")), 9);
    var javaA = new Directory(Files.createDirectories(temp.resolve("java-10")), 10);
    var N = Runtime.version().feature();
    var javaN = new Directory(Files.createDirectories(temp.resolve("java-preview")), N);
    assertLinesMatch(
        List.of(java8.toString(), java9.toString(), javaA.toString(), javaN.toString()),
        Directory.listOf(temp).stream().map(Directory::toString).collect(Collectors.toList()));
  }
}
