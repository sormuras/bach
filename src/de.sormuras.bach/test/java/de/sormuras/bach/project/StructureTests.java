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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.API;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StructureTests {
  @Test
  void empty() {
    var empty = API.emptyStructure();
    assertTrue(empty.realms().isEmpty());
    assertNull(empty.mainRealm());
    assertTrue(empty.library().requires().isEmpty());
    assertTrue(empty.toString().contains("realms"));
    assertTrue(empty.findRealm("foo").isEmpty());
    assertEquals(Optional.empty(), empty.toMainRealm());
    assertEquals(List.of(), empty.toRealmNames());
    assertEquals(Set.of(), empty.toDeclaredModuleNames());
  }
}
