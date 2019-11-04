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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.module.ModuleDescriptor;
import java.util.List;
import org.junit.jupiter.api.Test;

class BachTests {
  @Test
  void moduleDescriptorParsesVersion() {
    assertDoesNotThrow(() -> ModuleDescriptor.Version.parse(Bach.VERSION));
    assertThrows(IllegalArgumentException.class, () -> ModuleDescriptor.Version.parse(""));
    assertThrows(IllegalArgumentException.class, () -> ModuleDescriptor.Version.parse("-"));
    assertThrows(IllegalArgumentException.class, () -> ModuleDescriptor.Version.parse("master"));
    assertThrows(IllegalArgumentException.class, () -> ModuleDescriptor.Version.parse("ea"));
  }

  @Test
  void log() {
    var log = new Log();
    log.debug("debug");
    log.info("info");
    log.warn("warn");
    assertLinesMatch(List.of("debug", "info"), log.lines());
    assertLinesMatch(List.of("warn"), log.errors());
  }
}
