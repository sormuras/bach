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

package de.sormuras.bach.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.API;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DirectoryTests {

  @Test
  void empty() {
    var empty = API.emptyDirectory();
    assertEquals(Path.of("empty"), empty.path());
    assertEquals(Directory.Type.UNKNOWN, empty.type());
    assertEquals(0, empty.release());
    assertTrue(empty.toString().contains(Directory.class.getSimpleName()));
    assertEquals("? `empty`", empty.toMarkdown());
  }

  @Test
  void types() {
    assertTrue(Directory.Type.SOURCE.isSource());
    assertTrue(Directory.Type.SOURCE_WITH_ROOT_MODULE_DESCRIPTOR.isSource());

    assertFalse(Directory.Type.SOURCE.isSourceWithRootModuleDescriptor());
  }
}
