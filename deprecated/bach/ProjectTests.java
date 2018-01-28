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

import org.junit.jupiter.api.Test;

class ProjectTests {

  @Test
  void defaults() {
    Project project = new Project();
    // assertEquals(Paths.get("."), project.root);
    assertEquals("bach", project.name);
    assertEquals("1.0.0-SNAPSHOT", project.version);
    // assertEquals(Paths.get("src", "xxx", "java"), project.getModuleSourcePaths("xxx").get(0));
    // assertEquals(Paths.get("target", "bach"), project.getTargetPath());
    // assertEquals(Paths.get("target", "bach", "compiled", "x"), project.getModuleTargetPath("x"));
  }
}
