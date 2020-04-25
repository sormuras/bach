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
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import test.base.FileSystem;
import test.base.SwallowSystem;

class BachTests {

  @Test
  @ResourceLock(Resources.SYSTEM_PROPERTIES)
  void defaults() {
    var bach = new Bach();
    assertEquals("Bach.java " + Bach.VERSION, bach.toString());
    assertFalse(bach.getPrinter().printable(Level.ALL));
    assertFalse(bach.getPrinter().printable(Level.TRACE));
    assertFalse(bach.getPrinter().printable(Level.DEBUG));
    assertTrue(bach.getPrinter().printable(Level.INFO));
    assertTrue(bach.getPrinter().printable(Level.WARNING));
    assertTrue(bach.getPrinter().printable(Level.ERROR));
    assertNotNull(bach.getWorkspace());
    assertNotNull(bach.getHttpClient());
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
    var run = new Run();
    assertDoesNotThrow(run.bach()::hashCode);
    var printer = run.bach().getPrinter();
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
        run.log().lines());
  }

  @Test
  void buildEmptyProjectWithDefaultBuildTaskFailsWithProjectValidationError(@TempDir Path temp) {
    var run = new Run(temp);
    var error = assertThrows(AssertionError.class, () -> run.bach().build(API.emptyProject()));
    assertEquals("Build project empty 0 (Task) failed", error.getMessage());
    assertEquals("project validation failed: no unit present", error.getCause().getMessage());
    var tasks = 13;
    assertLinesMatch(
        List.of(
            ">> BACH INIT >>",
            "Execute " + tasks + " tasks",
            "+ Build project empty 0",
            "\t+ Print version of various foundation tools",
            ">> RUN TOOLS >>",
            "\t* Validate workspace",
            "Empty base directory " + temp,
            "\t* Print project",
            ">> PROJECT COMPONENTS >>",
            "\t* Validate project",
            "no unit present",
            "Task execution failed: java.lang.IllegalStateException: project validation failed: no unit present",
            "Executed 6 of " + tasks + " tasks",
            ">> ERROR >>"),
        run.log().lines());
  }

  @Test
  void writeSummaryFailsInReadOnlyDirectory(@TempDir Path temp) throws Exception {
    try {
      FileSystem.chmod(temp, false, false, false);
      new Run(temp).bach().build(API.emptyProject(), new Task("?"));
    } catch (Throwable throwable) {
      assertTrue(throwable.getMessage().contains(temp.toString()));
    } finally {
      FileSystem.chmod(temp, true, true, true);
    }
  }
}
