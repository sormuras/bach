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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.internal.Modules;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import test.base.Tree;

class SurveyTests {

  @Test
  void ofConstructor() {
    var requiredModules = new TreeMap<String, Version>();
    requiredModules.put("a", null);
    requiredModules.put("c", Version.parse("2"));
    assertABC(new Survey(Set.of("a", "b"), requiredModules));
  }

  @Test
  void ofModuleFinder(@TempDir Path temp) throws Exception {
    var a = declare(temp, "a", "module a {}");
    var b = declare(temp, "b", "module b { requires a; requires c;}");
    var c = declare(temp, "c", "module c {}");
    var javac = ToolProvider.findFirst("javac").orElseThrow();
    javac.run(System.out, System.err, a.toString());
    javac.run(System.out, System.err, "--module-version", "2", c.toString()); // c@2
    javac.run(System.out, System.err, "--module-path", temp.toString(), b.toString());
    Files.delete(temp.resolve("c/module-info.class")); // Make module "c" magically disappear...
    assertABC(Survey.of(ModuleFinder.of(temp)));
  }

  @Test
  void ofSystem() {
    var system = Survey.of(ModuleFinder.ofSystem());
    assertTrue(system.declaredModules().contains("java.base"));
    assertFalse(system.requiredModuleNames().contains("java.base")); // mandated are ignored
    assertTrue(system.declaredModules().size() > system.requiredModules().size());
  }

  private static Path declare(Path path, String name, String source) throws Exception {
    var directory = Files.createDirectory(path.resolve(name));
    return Files.writeString(directory.resolve("module-info.java"), source);
  }

  private static void assertABC(Survey survey) {
    assertEquals(Set.of("a", "b"), survey.declaredModules());
    assertEquals(Set.of("a", "c"), survey.requiredModuleNames());
    assertEquals(Optional.empty(), survey.requiredVersion("a"));
    assertEquals("2", survey.requiredVersion("c").orElseThrow().toString());
    assertThrows(FindException.class, () -> survey.requiredVersion("b"));
    assertThrows(FindException.class, () -> survey.requiredVersion("x"));
  }
}
