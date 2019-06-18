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

package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ActionTests {

  @ParameterizedTest
  @EnumSource(Action.Default.class)
  void performDefaultActionOnEmptyDirectory(Action.Default action, @TempDir Path empty) {
    var test = new TestRun(empty, empty.resolve("work"));
    var bach = new Bach(test);

    if (action.action == null) {
      assertThrows(NullPointerException.class, () -> action.perform(bach));
      return;
    }
    assertDoesNotThrow(() -> action.perform(bach));
    var arguments = new ArrayDeque<>(List.of("a", "z"));
    assertSame(action, action.consume(arguments));
    assertEquals("[a, z]", arguments.toString());
  }

  @Test
  void actionsForEmptyListReturnsDefaultActions() {
    assertEquals(List.of(Action.Default.HELP), Action.of(List.of()));
  }

  @Test
  void actionsForHelpReturnsDefaultAction() {
    assertEquals(
        List.of(Action.Default.HELP, Action.Default.HELP, Action.Default.HELP),
        Action.of(List.of("help", "Help", "HELP")));
  }

  @Test
  void actionForObjectFails() {
    var e =
        assertThrows(
            IllegalArgumentException.class,
            () -> Action.of("java.lang.Object", new ArrayDeque<>()));
    assertEquals(
        "class java.lang.Object doesn't implement interface de.sormuras.bach.Action",
        e.getMessage());
  }

  @Test
  void actionForJigsawBuilder() {
    var action = Action.of(JigsawBuilder.class.getName(), new ArrayDeque<>());
    assertSame(JigsawBuilder.class, action.getClass());
  }

  @Test
  void toolConsumesArgumentsAndCreatesNewActionInstance() {
    var action = Action.Default.TOOL;
    var arguments = new ArrayDeque<>(List.of("a", "z"));
    assertNotSame(action, action.consume(arguments));
    assertEquals("[]", arguments.toString());
  }

  @Test
  @SwallowSystem
  void help(SwallowSystem.Streams streams) {
    new Bach().help();
    assertLinesMatch(
        List.of(
            "Usage of Bach.java (" + Bach.VERSION + "):  java Bach.java [<action>...]",
            "Available default actions are:",
            // " build        Build modular Java project in base directory.",
            // " clean        Delete all generated assets - but keep caches intact.",
            // " erase        Delete all generated assets - and also delete caches.",
            " help         Print this help screen on standard out... F1, F1, F1!",
            // " launch       Start project's main program.",
            " sync         Resolve required external assets, like 3rd-party modules.",
            " tool         Run named tool consuming all remaining arguments:",
            "                tool <name> <args...>",
            "                tool java --show-version Program.java",
            "Project information",
            "name = bach",
            "version = " + Bach.VERSION),
        streams.outLines());
    assertEquals(0, streams.errLines().size(), streams.toString());
  }

  @Test
  void sync(@TempDir Path temp) throws Exception {
    var test = new TestRun(temp, temp);
    var bach = new Bach(test);

    var a = Files.write(temp.resolve("a.txt"), List.of("// a file"));
    var b = Files.write(temp.resolve("b.txt"), List.of("// another file"));
    var lib = temp.resolve("lib");
    var sub = Files.createDirectories(lib.resolve("sub"));
    Files.writeString(lib.resolve("module-uri.properties"), "a=" + a.toUri());
    Files.writeString(sub.resolve("module-uri.properties"), "a=" + a.toUri() + "\nb=" + b.toUri());

    test.log(System.Logger.Level.TRACE, "[1]");
    Action.Default.SYNC.perform(bach);

    var empty = Files.createDirectories(lib.resolve("empty"));
    Files.writeString(empty.resolve("module-uri.properties"), "# empty, i.e. no key-value pairs");
    test.log(System.Logger.Level.TRACE, "[2]");
    Action.Default.SYNC.perform(bach);

    assertLinesMatch(
        List.of(
            ">> INIT >>",
            "[1]",
            ">> DOWNLOAD >>",
            "Synchronized 3 module uri(s).",
            "[2]",
            ">> USE CACHE >>",
            "Synchronized 3 module uri(s)."),
        test.outLines());
    assertEquals(3, test.outLines().stream().filter(line -> line.startsWith("Downloaded")).count());
    assertEquals(3, test.outLines().stream().filter(line -> line.startsWith("Timestamp")).count());
    assertEquals(0, test.errLines().size(), test.toString());
  }
}
