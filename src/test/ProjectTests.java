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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProjectTests {

  private final Path dependencies = Paths.get("dependencies");
  private final Path target = Paths.get("target");
  private final Path mainDestination = target.resolve(Paths.get("main", "mods"));
  private final Path testDestination = target.resolve(Paths.get("test", "mods"));

  @Test
  void creatingModuleGroupWithSameNameFails() {
    Exception e =
        assertThrows(
            IllegalArgumentException.class,
            () -> Project.builder().newModuleGroup("name").end().newModuleGroup("name"));
    assertEquals("name already defined", e.getMessage());
  }

  @Test
  void manual() {
    var project =
        Project.builder()
            .name("Manual")
            .version("II")
            .target(target)
            // main
            .newModuleGroup("main")
            .destination(mainDestination)
            .moduleSourcePath(List.of(Paths.get("src", "main", "java")))
            .end()
            // test
            .newModuleGroup("test")
            .destination(testDestination)
            .moduleSourcePath(List.of(Paths.get("src", "test", "java")))
            .modulePath(List.of(mainDestination, dependencies))
            .patchModule(Map.of("hello", List.of(Paths.get("src/main/java/hello"))))
            .end()
            // done
            .build();
    assertEquals("Manual", project.name());
    assertEquals("II", project.version());
    assertEquals(Paths.get("target"), project.target());
    assertEquals("main", project.moduleGroup("main").name());
    assertEquals("test", project.moduleGroup("test").name());
    assertEquals(2, project.moduleGroups().size());

    var main = project.moduleGroup("main");
    assertEquals("main", main.name());
    assertEquals(mainDestination, main.destination());
    assertEquals(List.of(Paths.get("src", "main", "java")), main.moduleSourcePath());
    assertTrue(main.patchModule().isEmpty());
    assertTrue(main.modulePath().isEmpty());
  }
}
