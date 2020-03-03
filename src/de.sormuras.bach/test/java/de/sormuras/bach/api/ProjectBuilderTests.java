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

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProjectBuilderTests {

  final Project project = new Project.Builder().name("name").version("99").paths("").build();

  @Test
  void name() {
    assertEquals("name", project.name());
  }

  @Test
  void version() {
    assertEquals("99", project.version().toString());
  }

  @Test
  void toNameAndVersion() {
    assertEquals("name 99", project.toNameAndVersion());
  }

  @Test
  void paths() {
    var paths = project.paths();
    assertEquals(Path.of(""), paths.base());
    assertEquals(Path.of(".bach"), paths.out());
    assertEquals(Path.of("lib"), paths.lib());
    assertEquals(Path.of(".bach/first/more"), paths.out("first", "more"));
    assertEquals(Path.of(".bach/classes/realm"), paths.classes("realm"));
    assertEquals(Path.of(".bach/modules/realm"), paths.modules("realm"));
    assertEquals(Path.of(".bach/sources/realm"), paths.sources("realm"));
    assertEquals(Path.of(".bach/documentation/javadoc"), paths.javadoc());
  }
}
