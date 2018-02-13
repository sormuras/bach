/*
 * Bach - Java Shell Builder
 * Copyright (C) 2018 Christian Stein
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class BachTests {

  private final List<String> actualLogLines = new CopyOnWriteArrayList<>();
  private final Bach bach = createBach(actualLogLines);

  private Bach createBach(List<String> lines) {
    var bach = new Bach();
    bach.quiet = false;
    bach.logger = lines::add;
    return bach;
  }

  @Test
  void log() {
    bach.log("log %s", "test");
    assertEquals("log test", actualLogLines.get(0));
  }

  @Test
  void runExecutable() {
    var result = bach.run("command", "a", "b", "3");
    assertEquals(1, result);
    assertEquals("[run] command [a, b, 3]", actualLogLines.get(0));
  }

  @Test
  void runSequentially() {
    Stream<Supplier<Integer>> tasks = Stream.of(() -> task("1"), () -> task("2"), () -> task("3"));
    bach.run("run task sequentially", tasks);
    var expected =
        List.of(
            "[run] run task sequentially...",
            "1 begin",
            "1 done. .+",
            "2 begin",
            "2 done. .+",
            "3 begin",
            "3 done. .+",
            "[run] run task sequentially done.");
    assertLinesMatch(expected, actualLogLines);
  }

  @Test
  void runParallel() {
    Stream<Supplier<Integer>> tasks = Stream.of(() -> task("1"), () -> task("2"), () -> task("3"));
    bach.run("run task in parallel", tasks.parallel());
    var expected =
        List.of(
            "[run] run task in parallel...",
            ". begin",
            ". begin",
            ". begin",
            ". done. .+",
            ". done. .+",
            ". done. .+",
            "[run] run task in parallel done.");
    assertLinesMatch(expected, actualLogLines);
  }

  @Test
  void runThrowsIllegalStateExceptionOnNoneZeroResult() {
    Executable executable = () -> bach.run("23", Stream.of(() -> task("42", () -> 9)));
    Exception exception = assertThrows(IllegalStateException.class, executable);
    assertEquals("0 expected, but got: 9", exception.getMessage());
  }

  private int task(String name) {
    return task(name, () -> 0);
  }

  private int task(String name, IntSupplier result) {
    bach.log("%s begin", name);
    var millis = (long) (Math.random() * 500 + 100);
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.interrupted();
    }
    bach.log("%s done. %d", name, millis);
    return result.getAsInt();
  }
}
