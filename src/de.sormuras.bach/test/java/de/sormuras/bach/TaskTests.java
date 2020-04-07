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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.System.Logger.Level;
import java.util.List;
import org.junit.jupiter.api.Test;
import test.base.Log;

class TaskTests {

  @Test
  void executeTask() {
    var log = new Log();
    var bach = new Bach(new Printer.Default(log, Level.ALL), Workspace.of());
    bach.execute(new Task("Task"));
    assertLinesMatch(
        List.of(">> BACH INIT >>", "Execute 1 tasks", "* Task", "Executed 1 of 1 tasks"),
        log.lines());
  }

  @Test
  void executeThreeWaitTasksInParallel() {

    class Wait extends Task {

      private final int millis;

      Wait() {
        this(10);
      }

      Wait(int millis) {
        super("Thread.sleep(" + millis + ")");
        this.millis = millis;
      }

      @Override
      public void execute(Execution execution) throws Exception {
        Thread.sleep(millis);
        assertNotNull(execution.getBach());
        assertTrue(execution.isPrintable(Level.ALL));
      }
    }

    var log = new Log();
    var bach = new Bach(new Printer.Default(log, Level.ALL), Workspace.of());
    bach.execute(new Task("3x Wait", true, List.of(new Wait(), new Wait(), new Wait())));
    assertLinesMatch(
        List.of(
            ">> BACH INIT >>",
            "Execute 4 tasks",
            "+ 3x Wait",
            "\t* Thread.sleep(10)",
            "\t* Thread.sleep(10)",
            "\t* Thread.sleep(10)",
            "= 3x Wait",
            "Executed 4 of 4 tasks"),
        log.lines());
  }

  @Test
  void executeThrowingTask() {
    var log = new Log();
    var bach = new Bach(new Printer.Default(log, Level.ALL), Workspace.of());
    var task = API.taskOf("Throw", __ -> throwException("BäMM!"));
    var error = assertThrows(AssertionError.class, () -> bach.execute(task));
    assertEquals("Throw (NamedTask) failed", error.getMessage());
    assertEquals("BäMM!", error.getCause().getMessage());
    assertLinesMatch(
        List.of(
            ">> BACH INIT >>",
            "Execute 1 tasks",
            "* Throw",
            "Task execution failed: java.lang.Exception: BäMM!",
            "Executed 0 of 1 tasks",
            ">> ERROR >>"),
        log.lines());
  }

  private void throwException(String message) throws Exception {
    throw new Exception(message);
  }
}
