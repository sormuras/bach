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

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import test.base.Log;

class TaskTests {

  @Test
  void executeThreeWaitTasksInParallel() {

    class Wait extends Task {

      private final int millis;

      Wait() {
        this(10);
      }

      Wait(int millis) {
        super("Thread.sleep(" + millis + ")", false, List.of());
        this.millis = millis;
      }

      @Override
      public void execute(Execution execution) throws Exception {
        Thread.sleep(millis);
      }
    }

    var log = new Log();
    var bach = new Bach(log, true, false);
    bach.execute(new Task("3x Wait", true, List.of(new Wait(), new Wait(), new Wait())));
    assertLinesMatch(
        List.of(
            "P Bach.java .+ initialized",
            "P \tverbose=true",
            "P \tdry-run=false",
            "P Execute task: 3x Wait",
            "P + 3x Wait",
            "P \t* Thread.sleep(10)",
            "P \t* Thread.sleep(10)",
            "P \t* Thread.sleep(10)",
            "P = 3x Wait",
            "P Task Execution Overview",
            ">> OVERVIEW >>",
            "P Execution of 3 tasks took .+ ms"),
        log.lines());
  }

  @Test
  void executeThrowingTask() {

    class Throw extends Task {
      @Override
      public void execute(Execution execution) throws Exception {
        throw new Exception("BäMM!");
      }
    }

    var log = new Log();
    var bach = new Bach(log, true, false);
    assertThrows(AssertionError.class, () -> bach.execute(new Throw()));
    assertLinesMatch(
        List.of(
            "P Bach.java .+ initialized",
            "P \tverbose=true",
            "P \tdry-run=false",
            "P Execute task: Throw",
            "P * Throw"),
        log.lines());
  }
}
