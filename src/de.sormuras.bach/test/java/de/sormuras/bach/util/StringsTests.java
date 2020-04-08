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

package de.sormuras.bach.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StringsTests {

  @Nested
  class ToolStrings {

    @Test
    void withZeroArgumentsReturnsListOfSizeOne() {
      var strings = Strings.list("tool");
      assertEquals(1, strings.size(), strings.toString());
      assertEquals("tool", strings.get(0));
    }

    @Test
    void withOneArgumentReturnsListOfSizeOne() {
      var strings = Strings.list("tool", "--version");
      assertEquals(1, strings.size(), strings.toString());
      assertEquals("tool --version", strings.get(0));
    }

    @Test
    void withTwoArgumentReturnsListOfSizeThree() {
      var strings = Strings.list("tool", "--option", "value");
      assertEquals(3, strings.size(), strings.toString());
      assertLinesMatch(List.of("tool with 2 arguments:", "\t--option", "\t\tvalue"), strings);
    }

    @Test
    void withMoreThenTwoArgumentsReturnsListOfManyIndentedStrings() {
      var strings = Strings.list("tool", "-a", "1", "--b", "2", "-c", "--d");
      assertEquals(7, strings.size(), strings.toString());
      assertLinesMatch(
          List.of("tool with 6 arguments:", "\t-a", "\t\t1", "\t--b", "\t\t2", "\t-c", "\t--d"),
          strings);
    }
  }
}
