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

package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.spi.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResolverTests {

  @Test
  void ofConstructor() {
    var v2 = Version.parse("2");
    assertABC(new Resolver(Set.of("a", "b"), Map.of("a", Set.of(), "c", Set.of(v2))));
  }

  @Test
  void ofCommandLineStrings() {
    assertABC(Resolver.of(Set.of("a", "b"), Set.of("a", "c@2")));
  }

  @Test
  void ofModuleInfoSource() {
    assertABC(Resolver.of("module a {}", "module b { requires a; requires c/*2*/; }"));
  }

  @Test
  void ofParsingSourceFiles(@TempDir Path temp) throws Exception {
    declare(temp, "a", "module a {}");
    declare(temp, "b", "module b { requires a; requires c /*2*/;}");
    assertABC(Resolver.of(Set.of(temp)));
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
    assertABC(Resolver.of(ModuleFinder.of(temp)));
  }

  private static Path declare(Path path, String name, String source) throws Exception {
    var directory = Files.createDirectory(path.resolve(name));
    return Files.writeString(directory.resolve("module-info.java"), source);
  }

  private static void assertABC(Resolver resolver) {
    assertEquals(Set.of("a", "b"), resolver.getDeclaredModules());
    assertEquals(Set.of("a", "c"), resolver.getRequiredModules());
    assertEquals(Optional.empty(), resolver.getRequiredVersion("a"));
    assertEquals("2", resolver.getRequiredVersion("c").orElseThrow().toString());
    var e = assertThrows(NoSuchElementException.class, () -> resolver.getRequiredVersion("x"));
    assertEquals("Module x is not mapped", e.getMessage());
  }
}
