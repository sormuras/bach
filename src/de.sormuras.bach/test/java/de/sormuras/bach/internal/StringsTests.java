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

package de.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StringsTests {

  @Nested
  class Texts {
    @Test
    void threeLines() {
      var expected = String.join(System.lineSeparator(), "1", "2", "3");
      assertEquals(expected, Strings.text("1", "2", "3"));
      assertEquals(expected, Strings.text(List.of("1", "2", "3")));
      assertEquals(expected, Strings.text(Stream.of("1", "2", "3")));
    }

    @Test
    void threeIndentedLines() {
      var expected = String.join(System.lineSeparator(), "-1", "-2", "-3");
      assertEquals(expected, Strings.textIndent("-", "1", "2", "3"));
      assertEquals(expected, Strings.textIndent("-", List.of("1", "2", "3")));
      assertEquals(expected, Strings.textIndent("-", Stream.of("1", "2", "3")));
    }
  }

  @Nested
  class DurationStrings {
    @ParameterizedTest
    @CsvSource({"20.345s,PT20.345S", "26h 3m 4.567s,P1DT2H3M4.567S"})
    void check(String expected, String duration) {
      assertEquals(expected, Strings.toString(Duration.parse(duration)));
    }
  }

  @Nested
  class PathsStrings {
    @Test
    void examples() {
      assertEquals("", Strings.toString(List.of()));
      assertEquals("a", Strings.toString(List.of(Path.of("a"))));
      assertEquals(
          "a" + File.pathSeparator + "b", Strings.toString(List.of(Path.of("a"), Path.of("b"))));
    }
  }

  @Nested
  class ListToolNameAndArguments {

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

  @Nested
  class HexStrings {
    @Test
    void empty() {
      assertEquals("", Strings.hex(new byte[0]));
    }

    @ParameterizedTest
    @CsvSource({"'',''", "313233,123", "61206f,a o"})
    void examples(String expected, String example) {
      assertEquals(expected, Strings.hex(example.getBytes(StandardCharsets.UTF_8)));
    }
  }
}
