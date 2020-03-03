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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SourceTests {

  @Test
  void sourceViaFactory() {
    var source = Source.of(Path.of("src/main/java"));
    assertEquals("java", source.path().getFileName().toString());
    assertEquals(0, source.release());
    assertTrue(source.target().isEmpty());
    assertFalse(source.isTargeted());
    assertTrue(source.flags().isEmpty());
    assertFalse(source.isVersioned());
  }

  @Test
  void sourceWithTarget9AndVersioned() {
    var source = new Source(Path.of("src/main/java"), 9, Set.of(Source.Flag.VERSIONED));
    assertEquals("java", source.path().getFileName().toString());
    assertEquals(9, source.release());
    assertEquals(9, source.target().orElseThrow());
    assertTrue(source.isTargeted());
    assertTrue(source.flags().contains(Source.Flag.VERSIONED));
    assertTrue(source.isVersioned());
  }
}
