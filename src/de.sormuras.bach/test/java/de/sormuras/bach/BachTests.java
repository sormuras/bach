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
    assertFalse(bach.isVerbose());
    assertFalse(bach.isDryRun());
    assertNotNull(bach.getWorkspace());
  }

  @Test
  @SwallowSystem
  @ResourceLock(Resources.SYSTEM_PROPERTIES)
  void callMainMethodWithoutArguments(SwallowSystem.Streams streams) {
    try {
      System.setProperty("ry-run", "");
      Bach.main();
    } finally {
      System.clearProperty("ry-run");
    }
    assertTrue(streams.errors().isEmpty());
    assertTrue(streams.lines().contains("Bach.java " + Bach.VERSION));
  }

  @Test
  void printMessagesAtAllLevelsInVerboseMode() {
    var log = new Log();
    var bach = new Bach(log, true, true, Workspace.of());
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
            "P Bach.java .+ initialized",
            ">> CONFIGURATION >>",
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
  void buildEmptyProjectWithNoopBuildTask() {
    var log = new Log();
    var bach = new Bach(log, true, false, Workspace.of());
    assertDoesNotThrow(() -> bach.build(API.emptyProject(), API.taskOf("NOOP", __ -> {})));
    assertLinesMatch(
        List.of(
            ">> BACH INIT >>",
            "P ",
            "P Execute task: NOOP",
            ">> EXECUTION >>",
            "P Execution of 1 tasks took .+ ms"),
        log.lines());
  }

  @Test
  void buildEmptyProjectWithDefaultBuildTaskFailsWithProjectValidationError() {
    var log = new Log();
    var bach = new Bach(log, true, false, Workspace.of());
    var error = assertThrows(AssertionError.class, () -> bach.build(API.emptyProject()));
    assertEquals("Build project empty 0 failed", error.getMessage());
    assertEquals("project validation failed: no unit present", error.getCause().getMessage());
    assertLinesMatch(
        List.of(
            ">> BACH INIT >>",
            "P ",
            "P Execute task: Build project empty 0",
            "P + Build project empty 0",
            "P \t* Print project",
            ">> PROJECT COMPONENTS >>",
            "P \t* Check project state",
            "P Task execution failed: java.lang.IllegalStateException: project validation failed: no unit present"),
        log.lines());
  }
}
