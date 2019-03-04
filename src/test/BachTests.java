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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

class BachTests {

  @Test
  void constructDefaultInstance() {
    var bach = new Bach();

    assertFalse(bach.debug);
    assertEquals(Path.of(""), bach.base);
    assertNotNull(bach.log);
    assertNotNull(bach.log.out);
    assertNotNull(bach.log.err);
    assertSame(System.Logger.Level.INFO, bach.log.threshold);
  }

  @Test
  void constructInstanceInDebugModeAndDifferentBaseDirectory(@TempDir Path empty) {
    var bach = new Bach(true, empty);

    assertTrue(bach.debug);
    assertEquals(empty, bach.base);
    assertNotNull(bach.log);
    assertNotNull(bach.log.out);
    assertNotNull(bach.log.err);
    assertSame(System.Logger.Level.ALL, bach.log.threshold);
  }

  @Test
  @SwallowSystem
  void mainWithoutArguments(SwallowSystem.Streams streams) {
    assertDoesNotThrow((Executable) Bach::main);
    assertLinesMatch(List.of(), streams.outLines());
    assertLinesMatch(List.of(), streams.errLines());
  }

  @Test
  @SwallowSystem
  void mainWithArgumentBuild(SwallowSystem.Streams streams) {
    assertDoesNotThrow(() -> Bach.main("build"));
    assertLinesMatch(List.of(), streams.outLines());
    assertLinesMatch(List.of(), streams.errLines());
  }

  @Test
  @SwallowSystem
  void mainWithArgumentThatIsUnsupported(SwallowSystem.Streams streams) {
    var e = assertThrows(IllegalArgumentException.class, () -> Bach.main("foo"));
    assertLinesMatch(List.of(), streams.outLines());
    assertLinesMatch(List.of(), streams.errLines());

    assertEquals("No enum constant Bach.Action.Default.FOO", e.getMessage());
  }

  @Test
  void runEmptyCollectionOfActions() {
    var bach = new Bach(true, Path.of(""));
    var lines = new ArrayList<String>();
    bach.log.out = lines::add;

    bach.run(List.of());
    assertLinesMatch(List.of("Performing 0 action(s)..."), lines);
  }

  @Test
  void runNoopAction() {
    new Bach().run(List.of(bach -> {}));
  }

  @Test
  void runThrowingAction() {
    var bach = new Bach();
    var lines = new ArrayList<String>();
    bach.log.err = lines::add;

    var action = new ThrowingAction();
    var error = assertThrows(Error.class, () -> bach.run(List.of(action)));
    assertEquals("Action failed: " + action.toString(), error.getMessage());
    assertEquals(UnsupportedOperationException.class, error.getCause().getClass());
    assertEquals("123", error.getCause().getMessage());

    assertLinesMatch(List.of("123"), lines);
  }

  private static class ThrowingAction implements Bach.Action {

    @Override
    public void perform(Bach bach) {
      throw new UnsupportedOperationException("123");
    }
  }
}
