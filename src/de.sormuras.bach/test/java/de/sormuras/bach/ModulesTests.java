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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Builder;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModulesTests {

  @Nested
  class Origin {

    @Test
    void originOfJavaLangObjectIsJavaBase() {
      var expected = Object.class.getModule().getDescriptor().toNameAndVersion();
      assertEquals(expected, Modules.origin(new Object()));
    }

    @Test
    void originOfThisObjectIsTheLocationOfTheClassFileContainer() {
      var expected = getClass().getModule().getDescriptor().toNameAndVersion();
      assertEquals(expected, Modules.origin(this));
    }
  }

  @Nested
  class Describe {

    private ModuleDescriptor describe(String module, Consumer<Builder> consumer) {
      var builder = ModuleDescriptor.newModule(module);
      consumer.accept(builder);
      return builder.build();
    }

    private ModuleDescriptor describe(String source) {
      return Modules.newModule(source).build();
    }

    @Test
    void describeModuleFromCompilationUnit(@TempDir Path temp) throws Exception {
      var info = Files.writeString(temp.resolve("module-info.java"), "module a {}");
      var expected = describe("a", a -> {});
      assertEquals(expected, Modules.describe(info));
    }

    @Test
    void parsingArbitraryTextFails() {
      assertThrows(IllegalArgumentException.class, () -> Modules.newModule("C="));
    }

    @Test
    void minimalisticModuleDeclaration() {
      var actual = describe("module a{}");
      assertEquals(describe("a", a -> {}), actual);
    }

    @Test
    void moduleDeclarationWithRequires() {
      var actual = describe("module a{requires b;}");
      assertEquals(describe("a", a -> a.requires("b")), actual);
    }

    @Test
    void moduleDeclarationWithRequiresAndVersion() {
      var actual = describe("module a{requires b/*1.2*/;}");
      assertEquals(describe("a", a -> a.requires(Set.of(), "b", Version.parse("1.2"))), actual);
    }

    @Test
    void moduleDeclarationWithComments() {
      var actual = describe("open /*test*/ module a /*extends a*/ {}");
      assertEquals(describe("a", a -> {}), actual);
    }
  }

  @Nested
  class ModuleSourcePath {

    @Test
    void moduleSourcePathWithModuleNameAtTheBeginning() {
      var info = Path.of("a.b.c/module-info.java");
      var actual = Modules.moduleSourcePath(info, "a.b.c");
      assertEquals(Path.of(".").toString(), actual);
    }

    @Test
    void moduleSourcePathWithModuleNameAtTheBeginningWithOffset() {
      var info = Path.of("a.b.c/offset/module-info.java");
      var actual = Modules.moduleSourcePath(info, "a.b.c");
      assertEquals(Path.of(".", "offset").toString(), actual);
    }

    @Test
    void moduleSourcePathWithModuleNameAtTheEnd() {
      var info = Path.of("src/main/a.b.c/module-info.java");
      var actual = Modules.moduleSourcePath(info, "a.b.c");
      assertEquals(Path.of("src/main").toString(), actual);
    }

    @Test
    void moduleSourcePathWithNestedModuleName() {
      var info = Path.of("src/a.b.c/main/java/module-info.java");
      var actual = Modules.moduleSourcePath(info, "a.b.c");
      assertEquals(String.join(File.separator, "src", "*", "main", "java"), actual);
    }

    @Test
    void moduleSourcePathWithNonUniqueModuleNameInPath() {
      var info = Path.of("a/a/module-info.java");
      assertThrows(IllegalArgumentException.class, () -> Modules.moduleSourcePath(info, "a"));
    }

    @Test
    void moduleSourcePathWithoutModuleNameInPath() {
      var info = Path.of("a/a/module-info.java");
      assertThrows(IllegalArgumentException.class, () -> Modules.moduleSourcePath(info, "b"));
    }
  }
}
