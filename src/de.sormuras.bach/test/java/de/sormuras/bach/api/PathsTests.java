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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PathsTests {

  @ParameterizedTest(name = "path=\"{0}\"")
  @ValueSource(strings = {"", "root", "a/b/c"})
  void paths(Path path) {
    assertPaths(path);
  }

  @Test
  void temporaryOutAndLibraryPaths(@TempDir Path temp) {
    var base = Path.of("");
    var paths = new Paths(base, temp, Path.of("lib"));
    assertThrows(AssertionError.class, () -> assertPaths(base, paths));
    assertEquals(base.resolve(""), paths.base());
    assertEquals(base.resolve("lib"), paths.lib());
    assertEquals(temp, paths.out());
    assertEquals(temp.resolve("first/more"), paths.out("first", "more"));
    assertEquals(temp.resolve("classes/realm"), paths.classes("realm"));
    assertEquals(temp.resolve("modules/realm"), paths.modules("realm"));
    assertEquals(temp.resolve("sources/realm"), paths.sources("realm"));
    assertEquals(temp.resolve("documentation/javadoc"), paths.javadoc());
  }

  private static void assertPaths(Path base) {
    assertPaths(base, Paths.of(base));
  }

  private static void assertPaths(Path base, Paths paths) {
    assertEquals(base.resolve(""), paths.base());
    assertEquals(base.resolve(".bach"), paths.out());
    assertEquals(base.resolve("lib"), paths.lib());
    assertEquals(base.resolve(".bach/first/more"), paths.out("first", "more"));
    assertEquals(base.resolve(".bach/classes/realm"), paths.classes("realm"));
    assertEquals(base.resolve(".bach/modules/realm"), paths.modules("realm"));
    assertEquals(base.resolve(".bach/sources/realm"), paths.sources("realm"));
    assertEquals(base.resolve(".bach/documentation/javadoc"), paths.javadoc());
  }
}
