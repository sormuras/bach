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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ToolTests {

  @Test
  void empty() {
    var empty = Tool.of("empty");
    assertEquals("empty", empty.name());
    assertThrows(NoSuchElementException.class, () -> empty.get(Option.class));
    assertTrue(empty.toArgumentStrings().isEmpty());
  }

  @Nested
  class ArgumentsTests {
    @Test
    void empty() {
      var empty = new Arguments();
      assertTrue(empty.build().isEmpty());
    }

    @Test
    void touchAllAdders() {
      var builder =
          new Arguments("0", 0x1)
              .add(2)
              .add("key", "value")
              .add("alpha", "beta", "gamma")
              .add(true, "first")
              .add(true, "second", "more")
              .add(false, "suppressed")
              .forEach(List.of('a', 'b', 'c'), Arguments::add)
              .forEach(Set.of('e'), (args, e) -> args.add("e"));
      assertLinesMatch(
          List.of(
              "0", "1", "2", "key", "value", "alpha", "beta", "gamma", "first", "second", "more",
              "a", "b", "c", "e"),
          builder.build());
    }
  }
}
