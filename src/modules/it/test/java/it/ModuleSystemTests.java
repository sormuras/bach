/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

package it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.Bach;
import java.lang.module.ModuleDescriptor;
import java.util.List;
import java.util.Set;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;

class ModuleSystemTests {

  @Test
  void testModuleIsNamed() {
    assertTrue(getClass().getModule().isNamed());
    assertEquals("it", getClass().getModule().getName());
  }

  @Test
  void bachModule() {
    // exploded and jarred module fixtures
    var mandated = Set.of(ModuleDescriptor.Requires.Modifier.MANDATED);
    var transitive = Set.of(ModuleDescriptor.Requires.Modifier.TRANSITIVE);
    var version = Object.class.getModule().getDescriptor().version().orElseThrow();
    var expected =
        ModuleDescriptor.newModule("de.sormuras.bach")
            .exports("de.sormuras.bach")
            .requires(mandated, "java.base", version)
            .requires(Set.of(), "java.compiler", version)
            .requires(transitive, "java.net.http", version)
            .uses(ToolProvider.class.getName())
            .provides(ToolProvider.class.getName(), List.of("de.sormuras.bach.BachToolProvider"));
    // only the jarred module provides the following attributes
    var actual = Bach.class.getModule().getDescriptor();
    actual.version().ifPresent(__ -> expected.version(Bach.VERSION));
    actual.mainClass().ifPresent(__ -> expected.mainClass("de.sormuras.bach.Bach"));
    assertEquals(expected.build(), actual);
  }
}
