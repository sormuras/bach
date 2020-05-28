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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PathsTests {

  @Nested
  class IsRootTests {
    @TestFactory
    Stream<DynamicTest> fileSystemRootDirectoriesAreRoots() {
      var roots = FileSystems.getDefault().getRootDirectories();
      return StreamSupport.stream(roots.spliterator(), false)
          .map(path -> dynamicTest(path.toString(), () -> Paths.isRoot(path)));
    }
  }

  @Nested
  class IsMultiReleaseDirectoryTests {

    List<Path> findMultiReleaseDirectories(List<Path> directories) {
      var matches = new ArrayList<Path>();
      for (var directory : directories)
        if (Paths.isMultiReleaseDirectory(directory)) matches.add(directory);
      return List.copyOf(matches);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "doc/project/JigsawQuickStart",
          "doc/project/JigsawQuickStartWorld",
          "doc/project/MultiRelease/com.foo",
          "src",
          "src/bach",
          "src/de.sormuras.bach",
          "src/de.sormuras.bach/main",
          "src/de.sormuras.bach/test",
          "src/test.base/test",
          "src/test.preview/test-preview"
        })
    void singleRelease(Path directory) {
      var directories = Paths.list(directory, Files::isDirectory);
      assertEquals(List.of(), findMultiReleaseDirectories(directories));
    }

    @ParameterizedTest
    @ValueSource(strings = {"doc/project/MultiRelease/org.bar", "doc/project/MultiRelease/org.baz"})
    void multiRelease(Path directory) {
      var directories = Paths.list(directory, Files::isDirectory);
      assertTrue(findMultiReleaseDirectories(directories).size() >= 1);
    }
  }

  @Nested
  class ListTests {

    @Test
    void listingOfBaseDirectory() {
      var actual = Paths.list(Path.of(""), Files::isRegularFile);
      assertLinesMatch(
          List.of(".gitignore", ">> MORE FILES >>", "README.md", "build.bat", ">> more files >>"),
          actual.stream().map(Path::toString).collect(Collectors.toList()));
    }
  }

  @Nested
  class DequeTests {

    @Test
    void emptyPathYieldsAnEmptyDeque() {
      assertEquals("[]", Paths.deque(Path.of("")).toString());
    }

    @Test
    void reversed() {
      assertEquals("[c, b, a]", Paths.deque(Path.of("a", "b", "c")).toString());
    }
  }
}
