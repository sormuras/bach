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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.API;
import java.util.List;
import org.junit.jupiter.api.Test;

class UnitTests {

  @Test
  void empty() {
    var empty = API.emptyUnit();
    assertEquals("empty", empty.descriptor().name());
    assertEquals(0, empty.directories().size());
    assertTrue(empty.toString().contains("empty"));
    assertEquals("empty", empty.name());
    assertEquals(List.of(), empty.toRequiresNames());
  }

  @Test
  void factory() {
    var directory = API.emptyDirectory();
    var foo = API.newUnit("foo", directory);
    assertEquals("foo", foo.descriptor().name());
    assertEquals("foo", foo.name());
    assertSame(directory, foo.directories().get(0));
    assertEquals(List.of(), foo.toRequiresNames());
  }
}
