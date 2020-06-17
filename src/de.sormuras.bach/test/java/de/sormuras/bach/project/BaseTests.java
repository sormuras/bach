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

class BaseTests {

  @Test
  void defaults() {
    var base = Base.of();
    assertEquals(Path.of(""), base.directory());
    assertEquals(Path.of("README"), base.directory("README"));
    assertEquals(Path.of("lib"), base.libraries());
    assertEquals(Path.of("lib/foo@123.jar"), base.libraries("foo@123.jar"));
    assertEquals(Path.of(".bach/workspace"), base.workspace());
    assertEquals(Path.of(".bach/workspace/.locks"), base.workspace(".locks"));
  }

  @Test
  void custom() {
    var base = Base.of(Path.of("custom"));
    assertEquals(Path.of("custom"), base.directory());
    assertEquals(Path.of("custom/lib"), base.libraries());
    assertEquals(Path.of("custom/.bach", "workspace"), base.workspace());
  }

  @Test
  void classes() {
    var base = Base.of();
    assertEquals(base.workspace("classes/99"), base.classes("", 99));
    assertEquals(base.workspace("classes/99/module"), base.classes("", 99, "module"));
    assertEquals(base.workspace("classes-test/99"), base.classes("test", 99));
    assertEquals(base.workspace("classes-test/99/module"), base.classes("test", 99, "module"));
  }
}
