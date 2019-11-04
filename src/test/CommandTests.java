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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;
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
  void addPath() {
    var command = new Bach.Command("a").add(Path.of("a/b"));
    assertEquals(List.of("a" + File.separator + "b"), command.arguments);
    assertEquals("new Command(\"a\").add(Path.of(\"a/b\"))", command.toSource());
  }

  @Test
  void addListOfPath() {
    assertEquals(
        List.of("b", "c"),
        new Bach.Command("a").add("b", List.of(Path.of("c"))).arguments);
    var paths = new Bach.Command("a").add("b", List.of(Path.of("c"), Path.of("d")));
    assertEquals(
        List.of("b", "c" + File.pathSeparator + "d"),
        paths.arguments);
    assertEquals(List.of(".add(\"b\", Path.of(\"c\"), Path.of(\"d\"))"), paths.additions);
  }

  @Test
  void generateSourceForCommandWithoutArguments() {
    var command = new Bach.Command("empty");
    assertEquals("new Command(\"empty\")", command.toSource());
  }

  @Test
  void generateSourceForCommandWithSingleArgument() {
    var command = new Bach.Command("one").add("1");
    assertEquals("new Command(\"one\").add(\"1\")", command.toSource());
  }

  @Test
  void generateSourceForCommandWithTwoArguments() {
    var command = new Bach.Command("two").add("1", "2");
    assertEquals("new Command(\"two\").add(\"1\", \"2\")", command.toSource());
  }
}
