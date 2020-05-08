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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProjectTests {

  static Project zero() {
    return Project.newProject("Zero", "0").build();
  }

  @Test
  void defaults() {
    var project = zero();
    assertNotNull(project.toString());
    assertEquals("Zero 0", project.toTitleAndVersion());
    var base = project.base();
    assertEquals(Path.of(""), base.directory());
    assertEquals(Path.of(".bach/workspace"), base.workspace());
    assertEquals(Path.of("foo"), base.path("foo"));
    assertEquals(Path.of(".bach/workspace/foo"), base.workspace("foo"));
    assertEquals(Path.of(".bach/workspace/api"), base.api());
    assertEquals(Path.of(".bach/workspace/classes/realm"), base.classes("realm"));
    assertEquals(Path.of(".bach/workspace/classes/realm/a.b.c"), base.classes("realm", "a.b.c"));
    assertEquals(Path.of(".bach/workspace/image"), base.image());
    assertEquals(Path.of(".bach/workspace/modules/realm"), base.modules("realm"));
    var info = project.info();
    assertEquals("0", info.version().toString());
    var structure = project.structure();
    assertTrue(structure.realms().isEmpty());
  }

  @Nested
  class DocProject {
    @Test
    void walkJigsawQuickStart() {
      var base = Path.of("doc", "project", "JigsawQuickStart");
      var project =
          new Project.Builder()
              .base(Project.Base.of(base))
              .walk(
                  (tool, context) -> {
                    assertEquals("", context.realm());
                    assertNull(context.module());
                    if (tool instanceof Tool.Javac)
                      ((Tool.Javac) tool).getAdditionalArguments().add("--verbose");
                  })
              .build();
      assertSame(base, project.base().directory());
      assertEquals(base.resolve(".bach/workspace"), project.base().workspace());
      assertEquals("Project JigsawQuickStart 1-ea", project.toTitleAndVersion());
      assertEquals(Set.of("com.greetings"), project.toDeclaredModuleNames());
      assertEquals(Set.of(), project.toRequiredModuleNames());
      var realm = project.structure().realms().get(0);
      assertEquals("", realm.name());
      assertSame(Task.RunTool.class, realm.javac().getClass());
      var javac = realm.javac();
      assertTrue(javac.getLabel().contains("--verbose"));
      assertTrue(javac.getLabel().contains("com.greetings"));
    }

    @Test
    void walkJigsawQuickStartWorld() {
      var base = Path.of("doc", "project", "JigsawQuickStartWorld");
      var project = Project.newProject(base).build();
      assertSame(base, project.base().directory());
      assertEquals(base.resolve(".bach/workspace"), project.base().workspace());
      assertEquals("Project JigsawQuickStartWorld 1-ea", project.toTitleAndVersion());
      assertEquals(
          Set.of("com.greetings", "org.astro", "test.modules"), project.toDeclaredModuleNames());
      assertEquals(Set.of("com.greetings", "org.astro"), project.toRequiredModuleNames());
    }
  }
}
