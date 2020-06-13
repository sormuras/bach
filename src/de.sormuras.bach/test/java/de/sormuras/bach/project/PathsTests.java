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

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PathsTests {

  @Test
  void defaults() {
    var paths = Paths.of();
    assertEquals(Path.of(""), paths.base());
    assertEquals(Path.of("README"), paths.base("README"));
    assertEquals(Path.of("lib"), paths.library());
    assertEquals(Path.of("lib/junit@4.13.jar"), paths.library("junit@4.13.jar"));
    assertEquals(Path.of(".bach/workspace"), paths.workspace());
    assertEquals(Path.of(".bach/workspace/.locks"), paths.workspace(".locks"));
  }

  @Test
  void custom() {
    var paths = Paths.of(Path.of("custom"));
    assertEquals(Path.of("custom"), paths.base());
    assertEquals(Path.of("custom/lib"), paths.library());
    assertEquals(Path.of("custom/.bach", "workspace"), paths.workspace());
  }

  @Test
  void classes() {
    var paths = Paths.of();
    assertEquals(paths.workspace("classes/99"), paths.classes("", 99));
    assertEquals(paths.workspace("classes/99/module"), paths.classes("", 99, "module"));
    assertEquals(paths.workspace("classes-test/99"), paths.classes("test", 99));
    assertEquals(paths.workspace("classes-test/99/module"), paths.classes("test", 99, "module"));
  }
}
