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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
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
    var actual = Bach.Modules.describe("module a{}");
    assertEquals(ModuleDescriptor.newModule("a").build(), actual);
  }

  @Test
  void moduleDeclarationWithRequires() {
    var actual = Bach.Modules.describe("module a{requires b;}");
    assertEquals(ModuleDescriptor.newModule("a").requires("b").build(), actual);
  }

  @Test
  void moduleDeclarationWithRequiresAndVersion() {
    var actual = Bach.Modules.describe("module a{requires b/*1.2*/;}");
    assertEquals(
        ModuleDescriptor.newModule("a").requires(Set.of(), "b", Version.parse("1.2")).build(),
        actual);
  }

  @Test
  void moduleDeclarationWithProvides() {
    var actual = Bach.Modules.describe("module a{provides a.B with a.C;}");
    assertEquals(ModuleDescriptor.newModule("a").provides("a.B", List.of("a.C")).build(), actual);
  }

  @Test
  void moduleDeclarationWithProvidesTwoImplementations() {
    var actual = Bach.Modules.describe("module a{provides a.B with a.C,a.D;}");
    assertEquals(
        ModuleDescriptor.newModule("a").provides("a.B", List.of("a.C", "a.D")).build(), actual);
  }

  @Test
  void moduleDeclarationWithMainClass() {
    var actual = Bach.Modules.describe("// --main-class a.Main\nmodule a{}");
    assertEquals(ModuleDescriptor.newModule("a").mainClass("a.Main").build(), actual);
  }

  @Test
  void moduleDeclarationWithComments() {
    var actual = Bach.Modules.describe("open /*test*/ module a /*extends a*/ {}");
    assertEquals(ModuleDescriptor.newModule("a").build(), actual);
  }

  @Test
  void moduleSourcePathWithModuleNameAtTheEnd() {
    var actual = Bach.Modules.moduleSourcePath(Path.of("src/main/a.b.c"), "a.b.c");
    assertEquals(Path.of("src/main").toString(), actual);
  }

  @Test
  void moduleSourcePathWithNestedModuleName() {
    var actual = Bach.Modules.moduleSourcePath(Path.of("src/a.b.c/main/java"), "a.b.c");
    assertEquals(String.join(File.separator, "src", "*", "main", "java"), actual);
  }

  @Nested
  class Survey {

    @Test
    void ofConstructor() {
      var v2 = Version.parse("2");
      assertABC(new Bach.Modules.Survey(Set.of("a", "b"), Map.of("a", Set.of(), "c", Set.of(v2))));
    }

    @Test
    void ofModuleInfoSourceStrings() {
      assertABC(Bach.Modules.Survey.of("module a {}", "module b { requires a; requires c/*2*/; }"));
    }

    @Test
    void ofModuleInfoSourceFiles(@TempDir Path temp) throws Exception {
      var a = declare(temp, "a", "module a {}");
      var b = declare(temp, "b", "module b { requires a; requires c /*2*/;}");
      assertABC(Bach.Modules.Survey.of(Set.of(a, b)));
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
      assertABC(Bach.Modules.Survey.of(ModuleFinder.of(temp)));
    }

    @Test
    void ofSystem() {
      var system = Bach.Modules.Survey.of(ModuleFinder.ofSystem());
      assertTrue(system.getDeclaredModules().contains("java.base"));
      assertFalse(system.getRequiredModules().contains("java.base")); // mandated are ignored
      assertTrue(system.getDeclaredModules().size() > system.getRequiredModules().size());
    }
  }

  private static Path declare(Path path, String name, String source) throws Exception {
    var directory = Files.createDirectory(path.resolve(name));
    return Files.writeString(directory.resolve("module-info.java"), source);
  }

  private static void assertABC(Bach.Modules.Survey survey) {
    assertEquals(Set.of("a", "b"), survey.getDeclaredModules());
    assertEquals(Set.of("a", "c"), survey.getRequiredModules());
    assertEquals(Optional.empty(), survey.getRequiredVersion("a"));
    assertEquals("2", survey.getRequiredVersion("c").orElseThrow().toString());
    var e = assertThrows(Bach.UnmappedModuleException.class, () -> survey.getRequiredVersion("x"));
    assertEquals("Module x is not mapped", e.getMessage());
  }
}
