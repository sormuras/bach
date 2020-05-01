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

import java.io.File;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Builder;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ModulesTests {

  @Nested
  class ModuleSourcePathOption {
    @ParameterizedTest
    @CsvSource({
      ".               , foo/module-info.java",
      "src             , src/foo/module-info.java",
      "*/src           , foo/src/module-info.java",
      "src/*/main/java , src/foo/main/java/module-info.java"
    })
    void modulePatternFormForModuleFoo(String expected, Path path) {
      var actual = Modules.modulePatternForm(path, "foo");
      assertEquals(expected.replace('/', File.separatorChar), actual);
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
      return Modules.describe(source).build();
    }

    @Test
    void describeModuleFromCompilationUnit(@TempDir Path temp) throws Exception {
      var info = Files.writeString(temp.resolve("module-info.java"), "module a {}");
      var expected = describe("a", a -> {});
      assertEquals(expected, Modules.describe(info));
    }

    @Test
    void describeDirectoryFails() {
      assertThrows(UncheckedIOException.class, () -> Modules.describe(Path.of("")));
    }

    @Test
    void parsingArbitraryTextFails() {
      assertThrows(IllegalArgumentException.class, () -> Modules.describe("C="));
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
  class DeclaredAndRequired {
    @Test
    void empty() {
      assertTrue(Modules.declared(ModuleFinder.of()).isEmpty());
      assertTrue(Modules.declared(Stream.empty()).isEmpty());
      assertTrue(Modules.required(ModuleFinder.of()).isEmpty());
      assertTrue(Modules.required(Stream.empty()).isEmpty());
    }

    @Test
    void modulesWithoutRequires() {
      var modules = Set.of(module("a"), module("b"), module("c"));
      assertEquals(Set.of("a", "b", "c"), Modules.declared(modules.stream()));
      assertEquals(Set.of(), Modules.required(modules.stream()));
    }

    @Test
    void modulesWithRequires() {
      var modules = Set.of(module("a"), module("b", "a"), module("c", "b", "x"));
      assertEquals(Set.of("a", "b", "c"), Modules.declared(modules.stream()));
      assertEquals(Set.of("a", "b", "x"), Modules.required(modules.stream()));
    }

    @Test
    void systemModules() {
      assertTrue(Modules.declared(ModuleFinder.ofSystem()).contains("java.base"));
      assertTrue(Modules.required(ModuleFinder.ofSystem()).contains("java.logging"));
    }
  }

  @Nested
  class MainClassConvention {

    @Test
    void mainClassOfBachIsPresent() {
      var module = "de.sormuras.bach";
      var info = Path.of("src", module, "main", "java", "module-info.java");
      var mainClass = Modules.findMainClass(info, module);
      assertTrue(Files.isRegularFile(info), info.toUri().toString());
      assertTrue(mainClass.isPresent());
      assertEquals("de.sormuras.bach.Main", mainClass.get());
    }

    @Test
    void mainClassOfNotExistingModuleInfoIsNotPresent() {
      assertFalse(Modules.findMainClass(Path.of("module-info.java"), "a.b.c").isPresent());
    }
  }

  @Nested
  class MainModuleConvention {
    @Test
    void empty() {
      assertTrue(Modules.findMainModule(Stream.empty()).isEmpty());
    }

    @Test
    void single() {
      var a = ModuleDescriptor.newModule("a").mainClass("a.A").build();
      assertEquals("a", Modules.findMainModule(Stream.of(a)).orElseThrow());
    }

    @Test
    void multipleModuleWithSingletonMainClass() {
      var a = ModuleDescriptor.newModule("a").build();
      var b = ModuleDescriptor.newModule("b").mainClass("b.B").build();
      var c = ModuleDescriptor.newModule("c").build();
      assertEquals("b", Modules.findMainModule(Stream.of(a, b, c)).orElseThrow());
    }

    @Test
    void multipleModuleWithMultipleMainClasses() {
      var a = ModuleDescriptor.newModule("a").mainClass("a.A").build();
      var b = ModuleDescriptor.newModule("b").mainClass("b.B").build();
      var c = ModuleDescriptor.newModule("c").mainClass("c.C").build();
      assertTrue(Modules.findMainModule(Stream.of(a, b, c)).isEmpty());
    }
  }

  static ModuleDescriptor module(String name, String... requires) {
    var builder = ModuleDescriptor.newModule(name);
    for (var require : requires) builder.requires(require);
    return builder.build();
  }
}
