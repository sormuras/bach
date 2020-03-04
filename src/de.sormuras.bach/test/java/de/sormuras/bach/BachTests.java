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

package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.api.Project;
import de.sormuras.bach.execution.ExecutionContext;
import de.sormuras.bach.execution.ExecutionResult;
import de.sormuras.bach.execution.Task;
import de.sormuras.bach.execution.Tasks;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.base.Log;
import test.base.SwallowSystem;

class BachTests {

  @Test
  void versionNotZero() {
    assertNotEquals(Version.parse("0"), Bach.VERSION);
  }

  @Test
  @SwallowSystem
  void callMainMethodWithoutArguments(SwallowSystem.Streams streams) {
    Bach.main();
    assertTrue(streams.errors().isEmpty());
    assertTrue(streams.lines().contains("Bach.java " + Bach.VERSION));
  }

  @Test
  void printMessagesAtAllLevelsInVerboseMode() {
    var log = new Log();
    var bach = new Bach(log, true);
    assertDoesNotThrow(bach::hashCode);
    assertEquals("all", bach.print(Level.ALL, "all"));
    assertEquals("trace", bach.print(Level.TRACE, "trace"));
    assertEquals("debug", bach.print(Level.DEBUG, "debug"));
    assertEquals("info 123", bach.print("info %d", 123));
    assertEquals("warning", bach.print(Level.WARNING, "warning"));
    assertEquals("error", bach.print(Level.ERROR, "error"));
    assertEquals("off", bach.print(Level.OFF, "off"));
    assertLinesMatch(
        List.of(
            "P Bach initialized",
            "P all",
            "P trace",
            "P debug",
            "P info 123",
            "P warning",
            "P error",
            "P off"),
        log.lines());
  }

  @Test
  void executeLocalNoopTask() {
    var log = new Log();
    var bach = new Bach(log, true);
    var summary = new Summary(Project.builder().name("Noop").build());
    bach.execute(new Task("Noop", false, List.of()), summary);
    assertDoesNotThrow(summary::assertSuccessful);
  }

  @Test
  void executeLocalTasks(@TempDir Path temp) throws Exception {
    var log = new Log();
    var bach = new Bach(log, true);
    var noop = new Task("Noop", false, List.of());
    var fail =
        new Task("Fail", false, List.of()) {
          @Override
          public ExecutionResult execute(ExecutionContext execution) {
            assertSame(bach, execution.bach());
            execution.print(Level.DEBUG, "Debug %d", 1);
            execution.print(Level.ERROR, "Error %d", 1);
            var failed = execution.failed(new Error("X"));
            assertEquals(1, failed.code());
            assertEquals("Debug 1" + System.lineSeparator(), failed.out());
            assertEquals("Error 1" + System.lineSeparator(), failed.err());
            assertEquals("X", failed.throwable().getMessage());
            return failed;
          }
        };
    var summary = new Summary(Project.builder().name("local").paths(temp).build());
    bach.execute(new Task("group", false, List.of(noop, fail)), summary);
    assertThrows(AssertionError.class, summary::assertSuccessful);
    assertEquals(2, summary.countedChildlessTasks());
    assertEquals(4, summary.countedExecutionEvents());
    assertEquals("local", summary.project().toNameAndVersion());
    assertLinesMatch(
        List.of(
            "P Bach initialized",
            "P + group",
            "P * Noop",
            "P * Fail",
            "P Debug 1",
            "P Error 1",
            "P Error 1",
            "P = group"),
        log.lines());
    var markdown = summary.toMarkdown();
    assertLinesMatch(
        List.of(
            "# Summary",
            "",
            "## Project",
            ">> PROJECT >>",
            "## Task Execution Overview",
            "|    |Thread|Duration|Caption",
            "|----|-----:|-------:|-------",
            "|   +|     1|        | group",
            "\\Q|    |     1|\\E.+\\Q| **Noop** [...](#task-execution-details-\\E.+",
            "\\Q|   X|     1|\\E.+\\Q| **Fail** [...](#task-execution-details-\\E.+",
            "\\Q|   =|     1|\\E.+\\Q| group",
            ">> LEGEND >>",
            "## Task Execution Details",
            ">> TASK DETAILS >>",
            "## System Properties",
            ">> SYSTEM PROPERTIES LISTING >>"),
        markdown);
    var path = summary.write();
    assertEquals(markdown, Files.readAllLines(path));
  }

  @Nested
  class TasksTests {
    @Test
    void executeLocalToolProvider() {
      var task = new Tasks.RunToolProvider("Run noop tool", new NoopToolProvider(), "a", "b", "c");
      var log = new Log();
      var bach = new Bach(log, true);
      var summary = new Summary(Project.builder().name("Local").build());
      bach.execute(task, summary);
      assertDoesNotThrow(summary::assertSuccessful);
    }
  }
}
