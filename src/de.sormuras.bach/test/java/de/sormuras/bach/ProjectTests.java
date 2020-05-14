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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.internal.ModulesWalker;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProjectTests {

  static Project zero() {
    return new Project.Builder().title("Zero").version(Version.parse("0")).build();
  }

  static Project newProject(Path directory) {
    var base = Project.Base.of(directory);
    var directoryName = base.directory().toAbsolutePath().getFileName();
    var title = Optional.ofNullable(directoryName).map(Path::toString).orElse("Untitled");
    var builder = new Project.Builder().base(base).title(title).tuner(Project.Tuner::defaults);
    return ModulesWalker.walk(builder).build();
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
  class BuilderTests {
    @Test
    void defaults() {
      var builder = new Project.Builder();
      assertNotNull(builder.getBase());
      assertNotNull(builder.getInfo());
      assertNotNull(builder.getTuner());
    }
  }

  @Nested
  class DocProject {
    @Test
    void walkJigsawQuickStart() {
      var base = Path.of("doc", "project", "JigsawQuickStart");
      var project = newProject(base);
      assertSame(base, project.base().directory());
      assertEquals(base.resolve(".bach/workspace"), project.base().workspace());
      assertEquals("JigsawQuickStart 1-ea", project.toTitleAndVersion());
      assertEquals(Set.of("com.greetings"), project.toDeclaredModuleNames());
      assertEquals(Set.of(), project.toRequiredModuleNames());
      var realm = project.structure().realms().get(0);
      assertEquals("", realm.name());
      var javac = realm.javac();
      assertEquals("1-ea", javac.getVersionOfModulesThatAreBeingCompiled().toString());
      assertEquals("Compile module(s): [com.greetings]", javac.toLabel());
    }

    @Test
    void walkJigsawQuickStartWorld() {
      var base = Path.of("doc", "project", "JigsawQuickStartWorld");
      var project = newProject(base);
      assertSame(base, project.base().directory());
      assertEquals(base.resolve(".bach/workspace"), project.base().workspace());
      assertEquals("JigsawQuickStartWorld 1-ea", project.toTitleAndVersion());
      assertEquals(
          Set.of("com.greetings", "org.astro", "test.modules"), project.toDeclaredModuleNames());
      assertEquals(Set.of("com.greetings", "org.astro"), project.toRequiredModuleNames());
      var realms = project.structure().realms();
      assertEquals(2, realms.size(), realms.toString());
      var main = realms.get(0);
      assertEquals("main", main.name());
      assertEquals(2, main.units().size());
      var test = realms.get(1);
      assertEquals("test", test.name());
      assertEquals(2, test.units().size());
    }
  }
}
