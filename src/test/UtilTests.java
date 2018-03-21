/*
 * Bach - Java Shell Builder
 * Copyright (C) 2017 Christian Stein
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UtilTests {

  @Test
  void isJavaFile() {
    assertFalse(Util.isJavaFile(Paths.get("")));
    assertFalse(Util.isJavaFile(Paths.get("a/b")));
    assertTrue(Util.isJavaFile(Paths.get("src/test/UtilTests.java")));
  }

  @Test
  void isJarFile() {
    assertFalse(Util.isJarFile(Paths.get("")));
    assertFalse(Util.isJarFile(Paths.get("a/b")));
  }

  @Test
  void getPatchMap() {
    assertEquals(Map.of(), Util.getPatchMap(List.of(), List.of()));
    var main = Paths.get("demo/02-testing/src/main/java");
    var test = Paths.get("demo/02-testing/src/test/java");
    assertEquals(
        Map.of(
            "application", List.of(main.resolve("application")),
            "application.api", List.of(main.resolve("application.api"))),
        Util.getPatchMap(List.of(test), List.of(main)));
  }

  @Test
  void getClassPath() {
    assertEquals(List.of(), Util.getClassPath(List.of(), List.of()));
    var mods = List.of(Paths.get(".bach/resolved"));
    var deps = List.of(Paths.get(".bach/tools/google-java-format"));
    assertEquals(5, Util.getClassPath(mods, deps).size());
  }

  @Test
  void findDirectories() {
    var root = Paths.get(".").toAbsolutePath().normalize();
    var dirs = Util.findDirectories(root);
    assertTrue(dirs.contains(root.resolve("demo")));
    assertTrue(dirs.contains(root.resolve("src")));
  }

  @Test
  void findDirectoryNames() {
    var root = Paths.get(".").toAbsolutePath().normalize();
    var dirs = Util.findDirectoryNames(root);
    assertTrue(dirs.contains("demo"));
    assertTrue(dirs.contains("src"));
  }

  @Test
  void getPathOfModuleReference() {
    var moduleReference = ModuleFinder.ofSystem().find("java.base").orElseThrow();
    assertEquals(URI.create("jrt:/java.base"), Util.getPath(moduleReference).toUri());
  }
}
