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

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FolderTests {

  @Test
  void canonical() {
    var folder = new Folder(Path.of("src"), 0);
    assertEquals(Path.of("src"), folder.path());
    assertEquals(0, folder.release());
  }

  @Test
  void factory() {
    var folder = Folder.of(Path.of("java-123"));
    assertEquals(Path.of("java-123"), folder.path());
    assertEquals(123, folder.release());
  }
}
