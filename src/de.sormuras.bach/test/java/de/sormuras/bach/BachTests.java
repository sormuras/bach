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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.System.Logger.Level;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import test.base.Log;
import test.base.SwallowSystem;

class BachTests {

  @Test
  @ResourceLock(Resources.SYSTEM_PROPERTIES)
  void defaults() {
    var bach = new Bach();
    assertEquals("Bach.java " + Bach.VERSION, bach.toString());
    assertFalse(bach.getPrinter().isVerbose());
    assertNotNull(bach.getWorkspace());
  }

  @Test
  @SwallowSystem
  @ResourceLock(Resources.SYSTEM_PROPERTIES)
  void callMainMethodWithoutArguments(SwallowSystem.Streams streams) {
    Bach.main();
    assertTrue(streams.errors().isEmpty());
    assertTrue(streams.lines().contains("Bach.java " + Bach.VERSION));
  }

  @Test
  void printMessagesAtAllLevelsInVerboseMode() {
    var log = new Log();
    var bach = new Bach(new Printer.Default(log, Level.ALL), Workspace.of());
    assertDoesNotThrow(bach::hashCode);
    var printer = bach.getPrinter();
    printer.print(Level.ALL, "all");
    printer.print(Level.TRACE, "trace");
    printer.print(Level.DEBUG, "debug");
    printer.print(Level.INFO, "info");
    printer.print(Level.WARNING, "warning");
    printer.print(Level.ERROR, "error");
    printer.print(Level.OFF, "off");
    assertLinesMatch(
        List.of(
            "Bach.java .+ initialized",
            ">> CONFIGURATION >>",
            "all",
            "trace",
            "debug",
            "info",
            "warning",
            "error",
            "off"),
        log.lines());
  }

  @Test
  void buildEmptyProjectWithDefaultBuildTaskFailsWithProjectValidationError() {
    var log = new Log();
    var bach = new Bach(new Printer.Default(log, Level.ALL), Workspace.of());
    var error = assertThrows(AssertionError.class, () -> bach.build(API.emptyProject()));
    assertEquals("Build project empty 0 failed", error.getMessage());
    assertEquals("project validation failed: no unit present", error.getCause().getMessage());
    assertLinesMatch(
        List.of(
            ">> BACH INIT >>",
            "",
            "Execute task: Build project empty 0",
            "+ Build project empty 0",
            "\t* Print project",
            ">> PROJECT COMPONENTS >>",
            "\t* Check project state",
            "Task execution failed: java.lang.IllegalStateException: project validation failed: no unit present"),
        log.lines());
  }
}
