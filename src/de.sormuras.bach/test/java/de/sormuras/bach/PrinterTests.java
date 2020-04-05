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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.System.Logger.Level;
import java.util.List;
import org.junit.jupiter.api.Test;
import test.base.Log;
import test.base.SwallowSystem;

class PrinterTests {

  @Test
  @SwallowSystem
  void systemPrintLine(SwallowSystem.Streams streams) {
    Printer.systemPrintLine(Level.TRACE, "trace");
    Printer.systemPrintLine(Level.DEBUG, "debug");
    Printer.systemPrintLine(Level.INFO, "info");
    Printer.systemPrintLine(Level.WARNING, "warning");
    Printer.systemPrintLine(Level.ERROR, "error");
    assertLinesMatch(List.of("trace", "debug", "info"), streams.lines());
    assertLinesMatch(List.of("warning", "error"), streams.errors());
  }

  @Test
  void printMessagesAtAllLevelsInVerboseMode() {
    var log = new Log();
    var printer = new Printer.Default(log, Level.ALL);
    assertTrue(printer.isEnabled(Level.OFF));
    assertTrue(printer.isVerbose());
    printer.print(Level.TRACE, "trace");
    printer.print(Level.DEBUG, "debug");
    printer.print(Level.INFO, "info");
    printer.print(Level.WARNING, "warning");
    printer.print(Level.ERROR, "error");
    assertLinesMatch(List.of("trace", "debug", "info", "warning", "error"), log.lines());
  }
}
