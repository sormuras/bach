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

package de.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Builder;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModulesTests {

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
  class DeclaredAndRequired {
    @Test
    void empty() {
      assertTrue(Modules.declared(Stream.empty()).isEmpty());
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
  }

  static ModuleDescriptor module(String name, String... requires) {
    var builder = ModuleDescriptor.newModule(name);
    for(var require : requires) builder.requires(require);
    return builder.build();
  }
}
