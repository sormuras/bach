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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class TaskTests {

  @Test
  void defaults() {
    var task = new Task();
    assertEquals("Task", task.getLabel());
    assertEquals(List.of(), task.getList());
    assertNotNull(task.toString());
    assertDoesNotThrow(() -> task.execute(null));
  }

  @Test
  void throwingTaskThrows() {
    var message = "BäMM!";
    class Throwing extends Task {
      @Override
      public void execute(Bach bach) throws Exception {
        throw new Exception(message);
      }
    }
    var exception = assertThrows(Exception.class, () -> new Throwing().execute(null));
    assertEquals(message, exception.getMessage());
  }

  @Test
  void sequence() {
    class Sub extends Task {
      public Sub(int i) {
        super("" + i, List.of());
      }
    }
    var task = Task.sequence("123", new Sub(1), new Sub(2), new Sub(3));
    assertEquals("123", task.getLabel());
    assertEquals(3, task.getList().size());
  }
}
