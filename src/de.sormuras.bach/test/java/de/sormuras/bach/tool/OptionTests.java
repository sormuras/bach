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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OptionTests {

  @Nested
  class KeyValueTests {
    @Test
    void checkThatNullValueIsOmitted() {
      var option = new KeyValueOption<>("null", null);
      assertNull(option.value());
      var arguments = new Arguments();
      option.visit(arguments);
      assertLinesMatch(List.of("null"), arguments.build());
    }
  }

  @Nested
  class ObjectArrayTests {
    @Test
    void canonical() {
      var option = new ObjectArrayOption<>("a", "b", "c");
      assertArrayEquals(new String[] {"a", "b", "c"}, option.value());
      var arguments = new Arguments();
      option.visit(arguments);
      assertLinesMatch(List.of("a", "b", "c"), arguments.build());
    }
  }
}
