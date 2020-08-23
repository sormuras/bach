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

package test.modules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.sormuras.bach.project.Link;
import org.junit.jupiter.api.Test;

class LinkTests {

  @Test
  void moduleOfJitPackSingleModuleProject() {
    assertEquals(
        "https://jitpack.io/com/github/user/project/0/project-0.jar#version=0",
        Link.ofJitPack("foo", "user", "project", "0").uri());
  }

  @Test
  void modulesOfJitPackMultiModuleProject() {
    assertEquals(
        "https://jitpack.io/com/github/user/project/foo.bar/0/foo.bar-0.jar#version=0",
        Link.ofJitPack("foo.bar", "user", "project", "0", true).uri());
    assertEquals(
        "https://jitpack.io/com/github/user/project/foo.baz/0/foo.baz-0.jar#version=0",
        Link.ofJitPack("foo.baz", "user", "project", "0", true).uri());
  }
}
