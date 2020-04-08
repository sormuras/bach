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

package de.sormuras.bach.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Printer;
import de.sormuras.bach.Task;
import de.sormuras.bach.Workspace;
import java.lang.System.Logger.Level;
import java.util.List;
import org.junit.jupiter.api.Test;
import test.base.Log;

class RunToolTests {

  @Test
  void names() {
    assertEquals("Run tool", RunTool.name("tool"));
    assertEquals("Run tool a", RunTool.name("tool", "a"));
    assertEquals("Run tool a b", RunTool.name("tool", "a", "b"));
    assertEquals("Run tool a b ... (3 arguments)", RunTool.name("tool", "a", "b", "c"));
  }

  @Test
  void runNoopTask() {
    var log = new Log();
    var bach = new Bach(new Printer.Default(log, Level.ALL), Workspace.of());
    var noop = new NoopToolProvider();
    bach.execute(Task.run(noop));
    log.assertThatEverythingIsFine();
  }

  @Test
  void runFailingTask() {
    var log = new Log();
    var bach = new Bach(new Printer.Default(log, Level.ALL), Workspace.of());
    var noop = new NoopToolProvider(-1, true, true);
    assertThrows(AssertionError.class, () -> bach.execute(Task.run(noop, "a", "b", "c")));
    assertLinesMatch(
        List.of(
            ">> BACH BEGIN >>",
            "Task execution failed: java.lang.AssertionError: Run of noop failed with exit code: -1",
            "Error:",
            "\tan error line presented by NoopToolProvider",
            "Tool:",
            "\tnoop with 3 arguments:",
            "\t\ta",
            "\t\tb",
            "\t\tc",
            ">> BACH END. >>"),
        log.lines());
  }
}
