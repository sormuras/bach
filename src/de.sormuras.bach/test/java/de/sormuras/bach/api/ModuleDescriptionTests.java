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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ModuleDescriptionTests {

  @Test
  void empty() {
    var empty = API.emptyModuleDescription();
    assertEquals("empty", empty.descriptor().name());
    assertEquals(0, empty.directories().size());
    assertTrue(empty.toString().contains("empty"));
  }

  @Test
  void factory() {
    var directory = API.emptyDirectory();
    var description = ModuleDescription.of("foo", directory);
    assertEquals("foo", description.descriptor().name());
    assertSame(directory, description.directories().get(0));
  }
}
