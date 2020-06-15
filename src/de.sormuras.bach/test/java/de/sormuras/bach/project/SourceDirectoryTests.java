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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class SourceDirectoryTests {

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", ".", "..", "?", "abc", "java-9.0"})
  void releaseZero(String name){
    assertEquals(0, SourceDirectory.parseRelease(name));
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "0.1", "java1", "java-1"})
  void releaseOne(String name){
    assertEquals(1, SourceDirectory.parseRelease(name));
  }

  @ParameterizedTest
  @ValueSource(strings = {"10", "0.10", "java10", "java-10"})
  void releaseTen(String name){
    assertEquals(10, SourceDirectory.parseRelease(name));
  }

  @Test
  void checkSourceDirectoriesOfDocProjectMultiRelease() {
    var base = Path.of("doc", "project", "MultiRelease");

    var foo = SourceDirectory.of(base.resolve("com.foo"));
    assertEquals(0, foo.release());
    assertFalse(foo.isTargeted());
    assertTrue(foo.isModuleInfoJavaPresent());

    var bar8 = SourceDirectory.of(base.resolve("org.bar/java-8"));
    assertEquals(8, bar8.release());
    assertTrue(bar8.isTargeted());
    assertFalse(bar8.isModuleInfoJavaPresent());
    var bar9 = SourceDirectory.of(base.resolve("org.bar/java-9"));
    assertEquals(9, bar9.release());
    assertTrue(bar9.isTargeted());
    assertTrue(bar9.isModuleInfoJavaPresent());
    var bar11 = SourceDirectory.of(base.resolve("org.bar/java-11"));
    assertEquals(11, bar11.release());
    assertTrue(bar11.isTargeted());
    assertFalse(bar11.isModuleInfoJavaPresent());

    var baz10 = SourceDirectory.of(base.resolve("org.baz/java-10"));
    assertEquals(10, baz10.release());
    assertTrue(baz10.isTargeted());
    assertTrue(baz10.isModuleInfoJavaPresent());
  }
}
