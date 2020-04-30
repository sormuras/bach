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

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Test
  void useProjectBuilderTouchingAllComponents() {
    var project = Project.newProject("Title", "99").base(Project.Base.of()).build();
    assertEquals("Title 99", project.toTitleAndVersion());
    var base = project.base();
    assertEquals(Path.of(""), base.directory());
    assertEquals(Path.of(".bach/workspace"), base.workspace());
    assertEquals(Path.of("foo"), base.path("foo"));
    assertEquals(Path.of(".bach/workspace/foo"), base.workspace("foo"));
    assertEquals(Path.of(".bach/workspace/api"), base.api());
    assertEquals(Path.of(".bach/workspace/classes/realm"), base.classes("realm"));
    assertEquals(Path.of(".bach/workspace/classes/realm/module"), base.classes("realm", "module"));
    assertEquals(Path.of(".bach/workspace/image"), base.image());
    assertEquals(Path.of(".bach/workspace/modules/realm"), base.modules("realm"));
    var info = project.info();
    assertEquals("99", info.version().toString());
    assertNotNull(project.toString());
  }
}
