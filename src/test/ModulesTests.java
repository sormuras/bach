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

import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

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
}
