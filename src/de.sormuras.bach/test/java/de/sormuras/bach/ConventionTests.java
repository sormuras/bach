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

import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
  class MainModuleConvention {
    @Test
    void empty() {
      assertTrue(Convention.mainModule(Stream.empty()).isEmpty());
    }

    @Test
    void single() {
      var a = ModuleDescriptor.newModule("a").mainClass("a.A").build();
      assertEquals("a", Convention.mainModule(Stream.of(a)).orElseThrow());
    }

    @Test
    void multipleModuleWithSingletonMainClass() {
      var a = ModuleDescriptor.newModule("a").build();
      var b = ModuleDescriptor.newModule("b").mainClass("b.B").build();
      var c = ModuleDescriptor.newModule("c").build();
      assertEquals("b", Convention.mainModule(Stream.of(a, b, c)).orElseThrow());
    }

    @Test
    void multipleModuleWithMultipleMainClasses() {
      var a = ModuleDescriptor.newModule("a").mainClass("a.A").build();
      var b = ModuleDescriptor.newModule("b").mainClass("b.B").build();
      var c = ModuleDescriptor.newModule("c").mainClass("c.C").build();
      assertTrue(Convention.mainModule(Stream.of(a, b, c)).isEmpty());
    }
  }

  @Nested
  class JavaReleaseConvention {
    @ParameterizedTest
    @ValueSource(strings = {"", "1", "abc", "java", "module"})
    void returnsZero(String string) {
      assertEquals(0, Convention.javaReleaseFeatureNumber(string));
    }

    @ParameterizedTest
    @CsvSource({"0,java-0", "0,java-module", "1,java-1", "9,java-9", "10,java-10", "99,java-99"})
    void returnsNumber(int expected, String string) {
      assertEquals(expected, Convention.javaReleaseFeatureNumber(string));
    }

    @Test
    void returnRuntimeVersionFeature() {
      var expected = Runtime.version().feature();
      assertEquals(expected, Convention.javaReleaseFeatureNumber("java-preview"));
    }

    @Test
    void statisticsForEmptyStreamOfPaths() {
      var statistics = Convention.javaReleaseStatistics(Stream.empty());
      assertEquals(0, statistics.getCount());
      assertEquals(Integer.MAX_VALUE, statistics.getMin());
      assertEquals(Integer.MIN_VALUE, statistics.getMax());
      assertEquals(0.0, statistics.getAverage());
      assertEquals(0, statistics.getSum());
    }

    @Test
    void statisticsForSinglePathWithoutNumber() {
      var statistics = Convention.javaReleaseStatistics(Stream.of(Path.of("java")));
      assertEquals(1, statistics.getCount());
      assertEquals(0, statistics.getMin());
      assertEquals(0, statistics.getMax());
      assertEquals(0.0, statistics.getAverage());
      assertEquals(0, statistics.getSum());
    }

    @Test
    void statisticsForSinglePathWithNumber() {
      var statistics = Convention.javaReleaseStatistics(Stream.of(Path.of("java-17")));
      assertEquals(1, statistics.getCount());
      assertEquals(17, statistics.getMin());
      assertEquals(17, statistics.getMax());
      assertEquals(17.0, statistics.getAverage());
      assertEquals(17, statistics.getSum());
    }

    @Test
    void statisticsForMultiplePaths() {
      var paths = Stream.of(Path.of("java-8"), Path.of("java-10"), Path.of("java-06"));
      var statistics = Convention.javaReleaseStatistics(paths);
      assertEquals(3, statistics.getCount());
      assertEquals(6, statistics.getMin());
      assertEquals(10, statistics.getMax());
      assertEquals(8.0, statistics.getAverage());
      assertEquals(24, statistics.getSum());
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
      expected.add("org.hamcrest");
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
