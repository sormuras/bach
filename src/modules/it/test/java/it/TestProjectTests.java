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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.Project;
import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestProjectTests {

  @Test
  void jigsawGreetings(@TempDir Path temp) {
    var base = Path.of("src", "test-project", "jigsaw-greetings");
    var greetings = Project.ModuleUnit.of(base.resolve("src/com.greetings"));
    var main = Project.Realm.of("main", greetings);
    assertEquals(String.join(File.separator, base.toString(), "src"), main.moduleSourcePath);
    var library = new Project.Library(temp.resolve("lib"));
    var project =
        new Project(
            base,
            temp,
            "jigsaw-greetings",
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
  void jigsawWorld(@TempDir Path temp) {
    var base = Path.of("src", "test-project", "jigsaw-world");
    var greetings = Project.ModuleUnit.of(base.resolve("src/main/com.greetings"));
    var astro = Project.ModuleUnit.of(base.resolve("src/main/org.astro"));
    var main = Project.Realm.of("main", List.of(greetings, astro));
    assertEquals(
        String.join(File.separator, base.toString(), "src", "main"), main.moduleSourcePath);
    var library = new Project.Library(temp.resolve("lib"));
    var project =
        new Project(
            base,
            temp,
            "jigsaw-greetings",
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
  void multiReleaseMultiModule(@TempDir Path temp) {
    var base = Path.of("src", "test-project", "multi-release-multi-module");
    var a =
        new Project.ModuleUnit(
            Project.ModuleInfo.of(base.resolve("src/a/main/java-9/module-info.java")),
            List.of(
                Project.Source.of(base.resolve("src/a/main/java-8"), 8),
                Project.Source.of(base.resolve("src/a/main/java-9"), 9),
                Project.Source.of(base.resolve("src/a/main/java-11"), 11)),
            List.of(),
            Path.of("pom.xml"));
    var b = Project.ModuleUnit.of(base.resolve("src/b/main/java"));
    var c =
        new Project.ModuleUnit(
            Project.ModuleInfo.of(base.resolve("src/c/main/java-9/module-info.java")),
            List.of(
                Project.Source.of(base.resolve("src/c/main/java-8"), 8),
                Project.Source.of(base.resolve("src/c/main/java-9"), 9),
                Project.Source.of(base.resolve("src/c/main/java-10"), 10),
                Project.Source.of(base.resolve("src/c/main/java-11"), 11)),
            List.of(),
            Path.of("pom.xml"));
    var d = Project.ModuleUnit.of(base.resolve("src/d/main/java"));
    var main = Project.Realm.of("main", List.of(a, b, c, d));
    assertEquals(
        String.join(
            File.pathSeparator,
            String.join(File.separator, base.toString(), "src", "*", "main", "java-9"),
            String.join(File.separator, base.toString(), "src", "*", "main", "java")),
        main.moduleSourcePath);
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

    var target = project.target(main);
    for (var unit : main.units) {
      assertTrue(Files.exists(target.modularJar(unit)), unit.info.toString());
      assertTrue(Files.exists(target.sourcesJar(unit)), unit.info.toString());
    }
  }

  @Test
  void requiresAsm(@TempDir Path temp) {
    var base = Path.of("src", "test-project", "requires-asm");
    var a = Project.ModuleUnit.of(base.resolve("src/a/main/java"));
    assertEquals(
        ModuleDescriptor.newModule("a")
            .requires(Set.of(), "org.objectweb.asm", ModuleDescriptor.Version.parse("7.1"))
            .build(),
        a.info.descriptor());
    var main = Project.Realm.of("main", a);
    assertEquals(
        String.join(File.separator, base.toString(), "src", "*", "main", "java"),
        main.moduleSourcePath);
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
