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
import static org.junit.jupiter.api.Assertions.assertSame;

import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.Realm;
import de.sormuras.bach.project.Structure;
import de.sormuras.bach.project.Unit;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectTests {
  @Test
  void createSimpleProjectAndVerifyItsComponents() {
    var base = Path.of("simple");
    var realm = new Realm("realm");
    var unit = new Unit(realm, ModuleDescriptor.newModule("unit").version("1").build());
    var structure = new Structure(List.of(realm), List.of(unit));
    var project = new Project(base, "simple", Version.parse("0"), structure);
    assertEquals("simple", project.name());
    assertEquals("0", project.version().toString());
    var folder = project.folder();
    assertEquals(base, folder.base());
    assertEquals(base.resolve(".bach/out"), folder.out());
    assertEquals(base.resolve(".bach/out/README.md"), folder.out("README.md"));
    assertEquals(base.resolve(".bach/out/log"), folder.log());
    assertEquals(base.resolve(".bach/out/realm"), folder.realm("realm"));
    assertEquals(base.resolve(".bach/out/realm/modules"), folder.modules("realm"));
    assertEquals(base.resolve("lib"), folder.lib());
    assertSame(structure, project.structure());
    assertSame(unit, project.unit("realm", "unit").orElseThrow());
    assertEquals("1", project.version(unit).toString());
  }
}
