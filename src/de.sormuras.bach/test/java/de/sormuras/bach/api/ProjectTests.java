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

package de.sormuras.bach.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.Bach;
import java.lang.module.FindException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import test.base.Log;
import test.base.Tree;

class ProjectTests {

  @Nested
  class Project99 {

    final Project project = Projects.newProject("project", "99");

    @Test
    void name() {
      assertEquals("project", project.name());
    }

    @Test
    void version() {
      assertEquals("99", project.version().toString());
    }

    @Test
    void toNameAndVersion() {
      assertEquals("project 99", project.toNameAndVersion());
    }

    @Test
    void toJarName() {
      var unit = Projects.unit("unit", "");
      assertEquals("unit-99.jar", project.toJarName(unit, ""));
      assertEquals("unit-99-classifier.jar", project.toJarName(unit, "classifier"));
    }

    @Test
    void toModularJar() {
      var realm = Projects.realm("");
      var unit = Projects.unit("unit", "");
      var actual = project.toModularJar(realm, unit);
      assertEquals(Path.of(".bach/modules", "unit-99.jar"), actual);
    }

    @Test
    void paths() {
      var realm = Projects.realm("realm");
      var paths = project.paths();
      assertEquals(Path.of(""), paths.base());
      assertEquals(Path.of(".bach"), paths.out());
      assertEquals(Path.of("lib"), paths.lib());
      assertEquals(Path.of(".bach/first/more"), paths.out("first", "more"));
      assertEquals(Path.of(".bach/classes/realm"), paths.classes(realm));
      assertEquals(Path.of(".bach/modules/realm"), paths.modules(realm));
      assertEquals(Path.of(".bach/sources/realm"), paths.sources(realm));
      assertEquals(Path.of(".bach/documentation/javadoc"), paths.javadoc());
    }

    @Test
    void units() {
      assertTrue(project.structure().units().isEmpty());
    }

    @Test
    void realms() {
      assertTrue(project.structure().realms().isEmpty());
    }

    @Test
    void tuner() {
      var tuner = project.tuner();
      assertNotNull(tuner);
      assertSame(Tuner.class, tuner.getClass());
    }

    @Test
    void library() {
      var library = project.library();
      assertNotNull(library);
      assertTrue(library.requires().isEmpty());
      assertTrue(library.locators().isEmpty());
    }

    @Test
    void toStrings() {
      assertLinesMatch(
          List.of(
              "Project project 99",
              "\tname: project",
              "\tversion: 99",
              "\trealms: 0",
              "\tunits: 0"),
          project.toStrings());
    }
  }

  @Nested
  class ProjectWithAllBellsAndWhistles {

    final Project project = Projects.newProjectWithAllBellsAndWhistles();

    @Test
    void library() {
      var library = project.library();
      assertEquals(Set.of("bar", "foo"), library.requires());
      assertFalse(library.locators().isEmpty());
      var bar = uri(library, "bar");
      assertEquals(Maven.central("com.bar", "bar", "1"), bar);
      var foo = uri(library, "foo");
      assertEquals(Maven.central("org.foo", "foo", "2"), foo);
      var junit = uri(library, "junit");
      assertEquals(Maven.central("junit", "junit", "3.7"), junit);
    }

    public URI uri(Library library, String module) {
      for (var locator : library.locators()) {
        var located = locator.locate(module);
        if (located.isPresent()) return located.get().uri();
      }
      throw new FindException("Module " + module + " not locatable");
    }
  }

  @Nested
  class DocumentationProject {

    final Path root = Path.of("doc/project");

    @TestFactory
    Stream<DynamicTest> scan() throws Exception {
      return Files.walk(root, 1)
          .filter(Files::isDirectory)
          .filter(Predicate.not(root::equals))
          .map(this::scan);
    }

    private DynamicTest scan(Path base) {
      var scanner = Project.scanner(base);
      return DynamicTest.dynamicTest(base.toString(), base.toUri(), scanner::scan);
    }

    @Nested
    class JigsawQuickStart {

      @Test
      void checkCraftedProjectMatchesScannedProject() {
        var base = Path.of("doc/project", "jigsaw.quick.start");
        assertLinesMatch(
            Projects.docProjectJigsawQuickStart().toStrings(),
            Project.scanner(base).scan().build().toStrings());
      }

      @Test
      void runBuildAndCheckCreatedAssets(@TempDir Path out) {
        var base = Path.of("doc/project", "jigsaw.quick.start");
        var project = new Scanner(new Paths(base, out, out)).scan().build();
        var log = new Log();
        var bach = new Bach(log, true, false);
        var summary = bach.build(project);
        summary.assertSuccessful();
        assertSame(project, summary.project());
      }
    }

    @Nested
    class JigsawQuickStartWithJUnit {

      @Test
      void runBuildAndCheckCreatedAssets(@TempDir Path out) {
        var base = Path.of("doc/project", "jigsaw.quick.start.with.junit");
        var paths = new Paths(base, out.resolve("out"), out.resolve("lib"));
        var project = new Scanner(paths).scan().build();
        var log = new Log();
        var bach = new Bach(log, true, false);
        var summary = bach.build(project);
        summary.assertSuccessful();
        System.gc();
        assertLinesMatch(
            List.of(
                "org.apiguardian.api-1.1.0.jar",
                "org.junit.jupiter-5.6.1.jar",
                "org.junit.jupiter.api-5.6.1.jar",
                "org.junit.jupiter.engine-5.6.1.jar",
                "org.junit.jupiter.params-5.6.1.jar",
                "org.junit.platform.commons-1.6.1.jar",
                "org.junit.platform.console-1.6.1.jar",
                "org.junit.platform.engine-1.6.1.jar",
                "org.junit.platform.launcher-1.6.1.jar",
                "org.junit.platform.reporting-1.6.1.jar",
                "org.opentest4j-1.2.0.jar"),
            Tree.walk(paths.lib()));
        assertLinesMatch(
            List.of("com.greetings.jar", "org.astro.jar"), Tree.walk(paths.out("modules", "main")));
        assertLinesMatch(
            List.of("org.astro.jar", "test.base.jar", "test.modules.jar"),
            Tree.walk(paths.out("modules", "test")));
      }
    }
  }
}
