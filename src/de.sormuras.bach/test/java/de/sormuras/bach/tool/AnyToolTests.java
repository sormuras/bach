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
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AnyToolTests {

  @Test
  void empty() {
    var empty = new AnyTool("any");
    assertTrue(empty.args().isEmpty());
    assertLinesMatch(List.of("any"), empty.toStrings());
  }

  @Test
  void touchAllAdders() {
    var tool =
        new AnyTool("any", 0x0)
            .add(1)
            .add("key", "value")
            .add("alpha", "beta", "gamma")
            .add(true, "first")
            .add(true, "second", "more")
            .add(false, "suppressed")
            .forEach(List.of('a', 'b', 'c'), AnyTool::add);
    assertEquals("any", tool.name());
    assertLinesMatch(
        List.of(
            "0", "1", "key", "value", "alpha", "beta", "gamma", "first", "second", "more", "a", "b",
            "c"),
        tool.args());
  }
}
