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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.System.Logger.Level;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BachContext.class)
class LogTests {

  @Test
  void log(BachContext context) {
    var logger = context.bach.log.logger;
    logger.accept(Level.ALL, "a");
    logger.accept(Level.TRACE, "t");
    context.bach.log.debug("%s", "d"); // same as: logger.accept(Level.DEBUG, "d");
    context.bach.log.info("%s", "i"); // same as: logger.accept(Level.INFO, "i");
    logger.accept(Level.WARNING, "w");
    logger.accept(Level.ERROR, "e");
    assertLinesMatch(List.of("a", "t", "d", "i", "w", "e"), context.recorder.all);
    assertLinesMatch(List.of("a", "t", "d", "i", "w", "e"), context.recorder.level(Level.ALL));
    assertLinesMatch(List.of("t", "d", "i", "w", "e"), context.recorder.level(Level.TRACE));
    assertLinesMatch(List.of("d", "i", "w", "e"), context.recorder.level(Level.DEBUG));
    assertLinesMatch(List.of("i", "w", "e"), context.recorder.level(Level.INFO));
    assertLinesMatch(List.of("w", "e"), context.recorder.level(Level.WARNING));
    assertLinesMatch(List.of("e"), context.recorder.level(Level.ERROR));
    assertLinesMatch(List.of(), context.recorder.level(Level.OFF));
  }

  @Test
  void debug(BachContext context) {
    assertTrue(context.bach.log.debug());
    context.bach.log.debug("%s", "1");
    context.bach.log.level = Level.OFF;
    assertFalse(context.bach.log.debug());
    context.bach.log.debug("%s", "2");
    context.bach.log.level = Level.INFO;
    assertFalse(context.bach.log.debug());
    context.bach.log.debug("%s", "3");
    assertLinesMatch(List.of("1", "2", "3"), context.recorder.all);
    assertLinesMatch(List.of("1", "2", "3"), context.recorder.level(Level.DEBUG));
    assertLinesMatch(List.of(), context.recorder.level(Level.INFO));
  }

  @Test
  void capturingStandardSystemStreams() {
    var out = System.out;
    var err = System.err;
    try {
      var bytesOut = new ByteArrayOutputStream(2000);
      var bytesErr = new ByteArrayOutputStream(2000);
      System.setOut(new PrintStream(bytesOut));
      System.setErr(new PrintStream(bytesErr));
      var bach = new Bach(); // pristine instance w/o print stream capturing context
      bach.log.level = Level.INFO;
      bach.log.log(Level.ALL, "a");
      bach.log.log(Level.TRACE, "t");
      bach.log.debug("d");
      bach.log.info("i");
      bach.log.log(Level.WARNING, "w");
      bach.log.log(Level.ERROR, "e");
      var actualOut = List.of(bytesOut.toString().split("\\R"));
      var actualErr = List.of(bytesErr.toString().split("\\R"));
      assertLinesMatch(List.of("[INFO] i", "[WARNING] w"), actualOut);
      assertLinesMatch(List.of("[ERROR] e"), actualErr);
    } finally {
      System.setOut(out);
      System.setErr(err);
    }
  }
}
