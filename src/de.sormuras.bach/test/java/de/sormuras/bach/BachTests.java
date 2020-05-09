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

import de.sormuras.bach.internal.Logbook;
import java.io.PrintWriter;
import java.net.http.HttpClient;
import java.util.List;
import java.util.spi.ToolProvider;
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
    var projectMessage = "Zero 0";
    assertLinesMatch(List.of(initialMessage, projectMessage), logbook.messages());
    assertLinesMatch(
        List.of("T|" + initialMessage, "D|" + projectMessage), logbook.lines(Object::toString));
  }

  @Test
  void executeLocalTool() {
    class Local implements ToolProvider {

      @Override
      public String name() {
        return "local";
      }

      @Override
      public int run(PrintWriter out, PrintWriter err, String... args) {
        out.printf("args -> %s", String.join("-", args));
        return 0;
      }
    }

    var bach = zero();
    var task = new Task.RunTool(new Local(), "1", "2", "3");
    bach.execute(task);
    var logbook = ((Logbook) bach.getLogger());
    assertLinesMatch(
        List.of(">> INIT >>", "T|* local 1 2 3", "D|local 1 2 3", "D|args -> 1-2-3"),
        logbook.lines(Object::toString));
  }
}
