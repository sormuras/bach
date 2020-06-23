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

package de.sormuras.bach.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import java.util.List;

class CallTests {

  @Test
  void defaults() {
    var call = Call.tool("any");
    assertEquals("any", call.name());
    assertFalse(call.activated());
    assertTrue(call.arguments().isEmpty());
    assertLinesMatch(List.of(), call.toStrings());
  }

  @Test
  void withIterable() {
    var call = Call.tool("any");
    call = call.with(List.of(), (tool, object) -> tool.with(String.valueOf(object)));
    assertTrue(call.arguments().isEmpty());
    call = call.with(List.of(1, '2', "3"), (tool, object) -> tool.with(String.valueOf(object)));
    assertLinesMatch(List.of("1", "2", "3"), call.toStrings());
  }

  @Test
  void withOptionsSharingOneName() {
    var call = Call.tool("any").with("x").with("y").with("y").with("z", "y");
    assertTrue(call.activated());
    assertLinesMatch(List.of("x", "y", "y", "z", "y"), call.toStrings());
    var withoutY = call.without("y");
    var withoutYZ = withoutY.without("z");
    var withoutXYZ = withoutYZ.without("x");
    assertLinesMatch(List.of("x", "z", "y"), withoutY.toStrings());
    assertLinesMatch(List.of("x"), withoutYZ.toStrings());
    assertLinesMatch(List.of(), withoutXYZ.toStrings());
    assertFalse(withoutXYZ.activated());
  }
}
