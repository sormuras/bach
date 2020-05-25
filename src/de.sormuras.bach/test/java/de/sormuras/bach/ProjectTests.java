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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Test
  void defaults() {
    var project = Projects.zero();
    assertNotNull(project.toString());
    assertEquals("Zero 0", project.toTitleAndVersion());
    var base = project.base();
    assertEquals(Path.of(""), base.directory());
    assertEquals(Path.of("foo"), base.path("foo"));
    assertEquals(Path.of("lib"), base.lib());
    assertEquals(Path.of(".bach/workspace"), base.workspace());
    assertEquals(Path.of(".bach/workspace/foo"), base.workspace("foo"));
    assertEquals(Path.of(".bach/workspace/api"), base.api());
    assertEquals(Path.of(".bach/workspace/classes/realm"), base.classes("realm"));
    assertEquals(Path.of(".bach/workspace/classes/realm/a.b.c"), base.classes("realm", "a.b.c"));
    assertEquals(Path.of(".bach/workspace/image"), base.image());
    assertEquals(Path.of(".bach/workspace/modules/realm"), base.modules("realm"));
    assertEquals(Path.of(".bach/workspace/sources/realm"), base.sources("realm"));
    var info = project.info();
    assertEquals("Zero", info.title());
    assertEquals("0", info.version().toString());
    // library
    assertTrue(project.library().required().isEmpty());
    // realms
    assertTrue(project.realms().isEmpty());
  }

  @Nested
  class ProjectBuilderTests {

    @Test
    void allDirectSettersPermitNullAsAnArgument() {
      var builder = Project.builder().setBase(null).setInfo(null).setLibrary(null).setRealms(null);
      var project = builder.newProject();
      assertEquals("", project.base().directory().toString());
      assertEquals("Untitled 1-ea", project.toTitleAndVersion());
    }

    @Test
    void withCustomBaseDirectoryAndWorkspace() {
      var base = Path.of("base");
      var work = Path.of("work");
      var project = Project.builder().base(base).workspace(work).newProject();
      assertEquals(base, project.base().directory());
      assertEquals(work, project.base().workspace());
    }

    @Test
    void withCustomModuleMapping() {
      var project = Project.builder().map("foo", "https://foo.jar").requires("foo").newProject();
      assertTrue(project.toDeclaredModuleNames().isEmpty());
      assertTrue(project.toRequiredModuleNames().isEmpty());
      assertEquals(Set.of("foo"), project.library().required());
      assertEquals("https://foo.jar", project.library().locator().apply("foo"));
    }
  }
}
