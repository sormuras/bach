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

import static java.lang.module.ModuleDescriptor.Requires.Modifier.MANDATED;
import static java.lang.module.ModuleDescriptor.newModule;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import de.sormuras.bach.Bach;
import java.util.Set;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;

class ModuleSystemTests {

  @Test
  void testModuleIsNamedAndTheNameIsIT() {
    assumeTrue(getClass().getModule().isNamed(), "not running on the module path");
    assertEquals("it", getClass().getModule().getName());
  }

  @Test
  void bachModule() {
    var actual = Bach.class.getModule().getDescriptor();
    assumeTrue(actual != null, "not running on the module path");
    // exploded and jarred module fixtures
    var expected =
        newModule("de.sormuras.bach")
            .exports("de.sormuras.bach")
            .exports("de.sormuras.bach.project")
            .packages(Set.of("de.sormuras.bach.task", "de.sormuras.bach.util"))
            .uses(ToolProvider.class.getName())
        // .provides(ToolProvider.class.getName(), List.of("de.sormuras.bach.BachToolProvider"))
        ;
    // requires may contain compiled version
    if (Runtime.version().feature() <= 11) {
      var version = Object.class.getModule().getDescriptor().version().orElseThrow();
      expected
          .requires(Set.of(MANDATED), "java.base", version)
          .requires(Set.of(), "java.compiler", version)
      // .requires(Set.of(TRANSITIVE), "java.net.http", version)
      ;
    } else {
      expected.requires(Set.of(MANDATED), "java.base").requires(Set.of(), "java.compiler")
      // .requires(Set.of(TRANSITIVE), "java.net.http")
      ;
    }
    // only the jarred module provides the following attributes
    actual.version().ifPresent(__ -> expected.version(Bach.VERSION));
    actual.mainClass().ifPresent(__ -> expected.mainClass("de.sormuras.bach.Bach"));
    assertEquals(expected.build(), actual);
  }
}
