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

package de.sormuras.bach.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import test.base.SwallowSystem;

class ToolsTests {
  @Test
  void empty() {
    assertThrows(NoSuchElementException.class, () -> new Tools().get(""));
  }

  @Test
  void allFoundationToolsAreAvailable() {
    var tools = new Tools();
    assertDoesNotThrow(() -> tools.get("jar"));
    assertDoesNotThrow(() -> tools.get("javac"));
    assertDoesNotThrow(() -> tools.get("javadoc"));
    assertDoesNotThrow(() -> tools.get("javap"));
    assertDoesNotThrow(() -> tools.get("jdeps"));
    assertDoesNotThrow(() -> tools.get("jlink"));
    assertDoesNotThrow(() -> tools.get("jmod"));
  }

  @TestFactory
  Stream<DynamicTest> allToolProvidersAreNamed() {
    var names = new ArrayList<String>();
    new Tools().forEach(tool -> names.add(tool.name()));
    return names.stream().map(name -> dynamicTest(name, () -> assertNotNull(name)));
  }

  @Test
  @SwallowSystem
  void launchJavaVersion(SwallowSystem.Streams streams) {
    new Tools().launch("java", List.of("--version"), false);
    assertFalse(streams.lines().isEmpty(), "lines() is empty?");
    assertTrue(streams.errors().isEmpty(), "errors() not empty: " + streams.errors());
  }
}
