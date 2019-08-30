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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DemoTests {
  @Test
  void build() {
    var bach = new Probe(Path.of("demo"));
    assertDoesNotThrow(bach::build, "bach::build failed: " + bach);
    assertLinesMatch(List.of(">> BUILD >>"), bach.lines());
  }

  @Test
  void validate() {
    var bach = new Probe(Path.of("demo"));
    assertDoesNotThrow(bach::validate, "bach::validate failed: " + bach);
    assertTrue(bach.out.toString().isBlank());
  }
}
