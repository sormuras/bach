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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.util.List;
import org.junit.jupiter.api.Test;

class CommandTests {
  @Test
  void toStringArray() {
    assertArrayEquals(new String[0], new Bach.Command("empty").toStringArray());
    assertArrayEquals(new String[]{"1"}, new Bach.Command("one").add(1).toStringArray());
    assertArrayEquals(new String[]{"2", "2"}, new Bach.Command("two").add("2", 2).toStringArray());
  }

  @Test
  void generateSourceForCommandWithoutArguments() {
    var command = new Bach.Command("empty");
    var actual = new Bach.SourceGenerator().generate(command);
    assertLinesMatch(List.of("java.util.spi.ToolProvider.findFirst(\"empty\").orElseThrow().run(System.out, System.err)"), actual);
  }

  @Test
  void generateSourceForCommandWithSingleArgument() {
    var command = new Bach.Command("one", "1");
    var actual = new Bach.SourceGenerator().generate(command);
    assertLinesMatch(List.of("java.util.spi.ToolProvider.findFirst(\"one\").orElseThrow().run(System.out, System.err, \"1\")"), actual);
  }

  @Test
  void generateSourceForCommandWithTwoArguments() {
    var command = new Bach.Command("two", "1", "2");
    var actual = new Bach.SourceGenerator().generate(command);
    assertLinesMatch(List.of("java.util.spi.ToolProvider.findFirst(\"two\").orElseThrow().run(System.out, System.err, \"1\", \"2\")"), actual);
  }
}
