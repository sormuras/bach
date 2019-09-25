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

package it;

import de.sormuras.bach.Project;
import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestProjectTests {
  @Test
  void multiReleaseMultiModule(@TempDir Path temp) {
    var base = Path.of("src", "test-project", "multi-release-multi-module");
    var a =
        new Project.MultiReleaseUnit(
            base.resolve("src/a/main/java-9/module-info.java"),
            9,
            Map.of(
                8, base.resolve("src/a/main/java-8"),
                9, base.resolve("src/a/main/java-9"),
                11, base.resolve("src/a/main/java-11")),
            List.of(), // resources
            ModuleDescriptor.newModule("a").build());
    var b =
        new Project.ModuleUnit(
            base.resolve("src/b/main/java/module-info.java"),
            List.of(base.resolve("src/b/main/java")),
            List.of(), // resources
            ModuleDescriptor.newModule("b").build());
    var c =
        new Project.MultiReleaseUnit(
            base.resolve("src/c/main/java-9/module-info.java"),
            9,
            Map.of(
                8, base.resolve("src/c/main/java-8"),
                9, base.resolve("src/c/main/java-9"),
                10, base.resolve("src/c/main/java-10"),
                11, base.resolve("src/c/main/java-11")),
            List.of(), // resources
            ModuleDescriptor.newModule("c").build());
    var d =
        new Project.ModuleUnit(
            base.resolve("src/d/main/java/module-info.java"),
            List.of(base.resolve("src/d/main/java")),
            List.of(), // resources
            ModuleDescriptor.newModule("d").build());
    var main =
        new Project.Realm(
            "main",
            false,
            0,
            String.join(
                File.pathSeparator,
                String.join(File.separator, base.toString(), "src", "*", "main", "java"),
                String.join(File.separator, base.toString(), "src", "*", "main", "java-9")),
            Map.of("hydra", List.of("a", "c"), "jigsaw", List.of("b", "d")),
            Map.of("a", a, "b", b, "c", c, "d", d));
    var library = new Project.Library(temp);
    var project =
        new Project(
            base,
            temp,
            "multi-release-multi-module",
            ModuleDescriptor.Version.parse("0"),
            library,
            List.of(main));

    var bach = new Probe(project);
    try {
      bach.build();
    } catch (Throwable t) {
      bach.lines().forEach(System.out::println);
      bach.errors().forEach(System.err::println);
      Assertions.fail(t);
    }
    // bach.lines().forEach(System.out::println);
    bach.errors().forEach(System.err::println);
  }

  @Test
  void requiresAsm(@TempDir Path temp) {
    var base = Path.of("src", "test-project", "requires-asm");
    var a =
        new Project.ModuleUnit(
            base.resolve("src/a/main/java/module-info.java"),
            List.of(base.resolve("src/a/main/java")),
            List.of(), // resources
            ModuleDescriptor.newModule("a").requires("org.objectweb.asm").version("7.1").build());
    var main =
        new Project.Realm(
            "main",
            false,
            0,
            String.join(File.separator, base.toString(), "src", "*", "main", "java"),
            Map.of("jigsaw", List.of("a")),
            Map.of("a", a));
    var library = new Project.Library(temp.resolve("lib"));
    var project =
        new Project(
            base,
            temp,
            "requires-asm",
            ModuleDescriptor.Version.parse("0"),
            library,
            List.of(main));

    var bach = new Probe(project);
    try {
      bach.build();
    } catch (Throwable t) {
      bach.lines().forEach(System.out::println);
      bach.errors().forEach(System.err::println);
      Assertions.fail(t);
    }
    // bach.lines().forEach(System.out::println);
    bach.errors().forEach(System.err::println);
  }
}
