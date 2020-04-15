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

package de.sormuras.bach.project;

import static de.sormuras.bach.Assertions.assertToStringEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.API;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DirectoryTests {

  @Test
  void empty() {
    var empty = API.emptyDirectory();
    assertEquals(Path.of("empty"), empty.path());
    assertEquals(Directory.Type.UNKNOWN, empty.type());
    assertEquals(0, empty.release());
    assertTrue(empty.toString().contains(Directory.class.getSimpleName()));
    assertEquals("? `empty`", empty.toMarkdown());
  }

  @Test
  void types() {
    assertSame(Directory.Type.UNKNOWN, Directory.Type.of("123"));
    assertSame(Directory.Type.SOURCE, Directory.Type.of("java"));
    assertSame(Directory.Type.SOURCE, Directory.Type.of("java-123"));
    assertSame(Directory.Type.RESOURCE, Directory.Type.of("resource"));
    assertSame(Directory.Type.RESOURCE, Directory.Type.of("resources"));
  }

  @Nested
  class JavaReleaseConvention {
    @ParameterizedTest
    @ValueSource(strings = {"", "1", "abc", "java", "module"})
    void returnsZero(String string) {
      assertEquals(0, Directory.javaReleaseFeatureNumber(string));
    }

    @ParameterizedTest
    @CsvSource({"0,java-0", "0,java-module", "1,java-1", "9,java-9", "10,java-10", "99,java-99"})
    void returnsNumber(int expected, String string) {
      assertEquals(expected, Directory.javaReleaseFeatureNumber(string));
    }

    @Test
    void returnRuntimeVersionFeature() {
      var expected = Runtime.version().feature();
      assertEquals(expected, Directory.javaReleaseFeatureNumber("java-preview"));
    }

    @Test
    void statisticsForEmptyStreamOfPaths() {
      var statistics = Directory.javaReleaseStatistics(Stream.empty());
      assertEquals(0, statistics.getCount());
      assertEquals(Integer.MAX_VALUE, statistics.getMin());
      assertEquals(Integer.MIN_VALUE, statistics.getMax());
      assertEquals(0.0, statistics.getAverage());
      assertEquals(0, statistics.getSum());
    }

    @Test
    void statisticsForSinglePathWithoutNumber() {
      var statistics = Directory.javaReleaseStatistics(Stream.of(Path.of("java")));
      assertEquals(1, statistics.getCount());
      assertEquals(0, statistics.getMin());
      assertEquals(0, statistics.getMax());
      assertEquals(0.0, statistics.getAverage());
      assertEquals(0, statistics.getSum());
    }

    @Test
    void statisticsForSinglePathWithNumber() {
      var statistics = Directory.javaReleaseStatistics(Stream.of(Path.of("java-17")));
      assertEquals(1, statistics.getCount());
      assertEquals(17, statistics.getMin());
      assertEquals(17, statistics.getMax());
      assertEquals(17.0, statistics.getAverage());
      assertEquals(17, statistics.getSum());
    }

    @Test
    void statisticsForMultiplePaths() {
      var paths = Stream.of(Path.of("java-8"), Path.of("java-10"), Path.of("java-06"));
      var statistics = Directory.javaReleaseStatistics(paths);
      assertEquals(3, statistics.getCount());
      assertEquals(6, statistics.getMin());
      assertEquals(10, statistics.getMax());
      assertEquals(8.0, statistics.getAverage());
      assertEquals(24, statistics.getSum());
    }
  }

  @Nested
  class Factory {
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
      var java = mkdir(temp, "java");
      var directories = Directory.listOf(temp);
      assertToStringEquals(new Directory(java, Directory.Type.SOURCE, 0), directories.get(0));
    }

    @Test
    void listOfPathWithMultipleSubdirectoriesReturnsThem(@TempDir Path temp) throws Exception {
      var java8 = new Directory(mkdir(temp, "java-8"), Directory.Type.SOURCE, 8);
      var java9 = new Directory(mkdir(temp, "java-9"), Directory.Type.SOURCE, 9);
      var N = Runtime.version().feature();
      var javaN = new Directory(mkdir(temp, "java-preview"), Directory.Type.SOURCE, N);
      var resources = new Directory(mkdir(temp, "resources"), Directory.Type.RESOURCE, 0);
      assertLinesMatch(
          List.of(java8.toString(), java9.toString(), javaN.toString(), resources.toString()),
          Directory.listOf(temp).stream()
              .map(Directory::toString)
              .sorted()
              .collect(Collectors.toList()));
    }

    Path mkdir(Path root, String other) throws Exception {
      return Files.createDirectories(root.resolve(other));
    }
  }
}
