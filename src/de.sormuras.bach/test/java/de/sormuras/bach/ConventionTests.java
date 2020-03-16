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

package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConventionTests {

  @Nested
  class MainClassConvention {

    @Test
    void mainClassOfBachIsPresent() {
      var module = "de.sormuras.bach";
      var info = Path.of("src", module, "main", "java", "module-info.java");
      var mainClass = Convention.mainClass(info, module);
      assertTrue(Files.isRegularFile(info), info.toUri().toString());
      assertTrue(mainClass.isPresent());
      assertEquals("de.sormuras.bach.Main", mainClass.get());
    }

    @Test
    void mainClassOfNotExistingModuleInfoIsNotPresent() {
      assertFalse(Convention.mainClass(Path.of("module-info.java"), "a.b.c").isPresent());
    }
  }

  @Nested
  class AmendJUnitTestEnginesConvention {
    @Test
    void emptyRemainsEmpty() {
      var set = new TreeSet<String>();
      Convention.amendJUnitTestEngines(set);
      assertTrue(set.isEmpty());
    }

    @Test
    void junitTriggersVintageEngine() {
      var actual = new TreeSet<String>();
      actual.add("junit");
      Convention.amendJUnitTestEngines(actual);
      var expected = new TreeSet<String>();
      expected.add("junit");
      expected.add("org.junit.vintage.engine");
      assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "module={0}")
    @ValueSource(strings = {"org.junit.jupiter", "org.junit.jupiter.api"})
    void moduleTriggersJupiterEngine(String module) {
      var actual = new TreeSet<String>();
      actual.add(module);
      Convention.amendJUnitTestEngines(actual);
      var expected = new TreeSet<String>();
      expected.add(module);
      expected.add("org.junit.jupiter.engine");
      assertEquals(expected, actual);
    }
  }

  @Nested
  class AmendJUnitPlatformConsoleConvention {
    @Test
    void emptyRemainsEmpty() {
      var empty = new TreeSet<String>();
      Convention.amendJUnitPlatformConsole(empty);
      assertTrue(empty.isEmpty());
    }

    @ParameterizedTest(name = "module={0}")
    @ValueSource(
        strings = {
          "org.junit.jupiter.engine",
          "org.junit.vintage.engine",
          "org.junit.platform.engine"
        })
    void moduleTriggersJUnitPlatformConsole(String module) {
      var actual = new TreeSet<String>();
      actual.add(module);
      Convention.amendJUnitPlatformConsole(actual);
      var expected = new TreeSet<String>();
      expected.add(module);
      expected.add("org.junit.platform.console");
      assertEquals(expected, actual);
    }
  }
}
