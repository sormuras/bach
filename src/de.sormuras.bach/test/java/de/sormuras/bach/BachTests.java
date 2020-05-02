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

import de.sormuras.bach.util.Logbook;
import java.net.http.HttpClient;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class BachTests {

  static Bach zero() {
    var logbook = new Logbook(__ -> {}, true, false);
    var zero = Project.newProject("Zero", "0").build();
    return new Bach(logbook, zero, HttpClient.newBuilder()::build);
  }

  @Test
  void defaults() {
    var bach = zero();
    var expectedStringRepresentation = "Bach.java " + Bach.VERSION;
    assertNotNull(bach.getLogger());
    assertEquals("0", bach.getProject().info().version().toString());
    assertNotNull(bach.getHttpClient());
    assertEquals(expectedStringRepresentation, bach.toString());

    var logbook = ((Logbook) bach.getLogger());
    var initialMessage = "Initialized " + expectedStringRepresentation;
    var projectMessage =
        String.join(
            System.lineSeparator() + "\t",
            "Project",
            "title: Zero",
            "version: 0",
            "realms: 0",
            "units: 0");
    assertLinesMatch(List.of(initialMessage, projectMessage), logbook.messages());
    assertLinesMatch(
        List.of("TRACE|" + initialMessage, "DEBUG|" + projectMessage),
        logbook.lines(Object::toString));
  }

  @Test
  void executeTool() {
    var bach = zero();
    var task = Task.runTool("javac", "--version");
    bach.execute(task);
    var logbook = ((Logbook) bach.getLogger());
    assertLinesMatch(
        List.of(
            ">> INIT >>",
            "TRACE|* javac --version",
            "DEBUG|javac --version",
            Pattern.quote("DEBUG|javac ") + ".+"),
        logbook.lines(Object::toString));
  }
}
