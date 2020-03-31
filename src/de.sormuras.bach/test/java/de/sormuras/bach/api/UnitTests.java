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

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class UnitTests {

  @Test
  void canonical() {
    var descriptor = ModuleDescriptor.newModule("foo").build();
    var unit = new Unit(descriptor, List.of());
    assertEquals("foo", unit.descriptor().name());
    assertEquals(0, unit.folders().size());
    assertTrue(unit.toString().contains("foo"));
  }

  @Test
  void factory() {
    var folder = Folder.of(Path.of("java-123"));
    var unit = Unit.of("foo", folder);
    assertEquals("foo", unit.descriptor().name());
    assertSame(folder, unit.folders().get(0));
  }
}
