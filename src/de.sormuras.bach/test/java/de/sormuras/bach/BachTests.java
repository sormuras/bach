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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.util.Logbook;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import test.base.SwallowSystem;

class BachTests {

  @Test
  void defaults() {
    var bach = new Bach();
    var expectedStringRepresentation = "Bach.java " + Bach.VERSION;
    assertEquals(expectedStringRepresentation, bach.toString());
    assertNotNull(bach.getLogger());
    assertNotNull(bach.getHttpClient());

    var logbook = ((Logbook) bach.getLogger());
    var initialMessage = "Initialized " + expectedStringRepresentation;
    assertLinesMatch(List.of(initialMessage), logbook.messages());
    assertLinesMatch(List.of("TRACE|" + initialMessage), logbook.lines(this::toLine));
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
  void executeTool() {
    var bach = new Bach();
    var task = Task.runTool("javac", "--version");
    bach.execute(task);
    var logbook = ((Logbook) bach.getLogger());
    assertLinesMatch(
        List.of(
            ">> INIT >>",
            "TRACE|* javac --version",
            "DEBUG|javac --version",
            Pattern.quote("DEBUG|javac ") + ".+"),
        logbook.lines(this::toLine));
  }

  String toLine(Logbook.Entry entry) {
    var thrown = entry.thrown() == null ? "" : " -> " + entry.thrown();
    return entry.level().name() + '|' + entry.message() + thrown;
  }
}
