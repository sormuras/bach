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

package de.sormuras.bach.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModulesTests {
  @Test
  void minimalisticModuleDeclaration() {
    var actual = Modules.describe("module a{}");
    assertEquals(ModuleDescriptor.newModule("a").build(), actual);
  }

  @Test
  void moduleDeclarationWithRequires() {
    var actual = Modules.describe("module a{requires b;}");
    assertEquals(ModuleDescriptor.newModule("a").requires("b").build(), actual);
  }

  @Test
  void moduleDeclarationWithRequiresAndVersion() {
    var actual = Modules.describe("module a{requires b/*1.2*/;}");
    assertEquals(
        ModuleDescriptor.newModule("a").requires(Set.of(), "b", Version.parse("1.2")).build(),
        actual);
  }

  @Test
  void moduleDeclarationWithProvides() {
    var actual = Modules.describe("module a{provides a.B with a.C;}");
    assertEquals(ModuleDescriptor.newModule("a").provides("a.B", List.of("a.C")).build(), actual);
  }

  @Test
  void moduleDeclarationWithProvidesTwoImplementations() {
    var actual = Modules.describe("module a{provides a.B with a.C,a.D;}");
    assertEquals(
        ModuleDescriptor.newModule("a").provides("a.B", List.of("a.C", "a.D")).build(), actual);
  }

  @Test
  void moduleDeclarationWithMainClass() {
    var actual = Modules.describe("// --main-class a.Main\nmodule a{}");
    assertEquals(ModuleDescriptor.newModule("a").mainClass("a.Main").build(), actual);
  }

  @Test
  void moduleDeclarationWithComments() {
    var actual = Modules.describe("open /*test*/ module a /*extends a*/ {}");
    assertEquals(ModuleDescriptor.newModule("a").build(), actual);
  }

  @Nested
  class Survey {

    @Test
    void ofConstructor() {
      var v2 = Version.parse("2");
      assertABC(new Modules.Survey(Set.of("a", "b"), Map.of("a", Set.of(), "c", Set.of(v2))));
    }

    @Test
    void ofModuleInfoSourceStrings() {
      assertABC(Modules.Survey.of("module a {}", "module b { requires a; requires c/*2*/; }"));
    }

    @Test
    void ofModuleInfoSourceFiles(@TempDir Path temp) throws Exception {
      var a = declare(temp, "a", "module a {}");
      var b = declare(temp, "b", "module b { requires a; requires c /*2*/;}");
      assertABC(Modules.Survey.of(Set.of(a, b)));
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
      assertABC(Modules.Survey.of(ModuleFinder.of(temp)));
    }

    @Test
    void ofSystem() {
      var system = Modules.Survey.of(ModuleFinder.ofSystem());
      assertTrue(system.declaredModules().contains("java.base"));
      assertFalse(system.requiredModules().contains("java.base")); // mandated are ignored
      assertTrue(system.declaredModules().size() > system.requiredModules().size());
    }
  }

  private static Path declare(Path path, String name, String source) throws Exception {
    var directory = Files.createDirectory(path.resolve(name));
    return Files.writeString(directory.resolve("module-info.java"), source);
  }

  private static void assertABC(Modules.Survey survey) {
    assertEquals(Set.of("a", "b"), survey.declaredModules());
    assertEquals(Set.of("a", "c"), survey.requiredModules());
    assertEquals(Optional.empty(), survey.requiredVersion("a"));
    assertEquals("2", survey.requiredVersion("c").orElseThrow().toString());
    assertThrows(Modules.UnmappedModuleException.class, () -> survey.requiredVersion("b"));
    assertThrows(Modules.UnmappedModuleException.class, () -> survey.requiredVersion("x"));
  }
}
