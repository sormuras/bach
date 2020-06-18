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

import de.sormuras.bach.project.Project;
import de.sormuras.bach.tool.Argument;
import de.sormuras.bach.tool.Call;
import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;

class BachTests {

  @Test
  void defaults() {
    var bach = Bach.of(Project.of("zero", "0"));
    var expectedStringRepresentation = "Bach.java " + Bach.VERSION;
    assertEquals(expectedStringRepresentation, bach.toString());
  }

  @Test
  void executeLocalTool() {
    class Local implements Call<Local>, ToolProvider {

      final List<Argument> arguments;

      Local() {
        this(List.of());
      }

      Local(List<Argument> arguments) {
        this.arguments = arguments;
      }

      @Override
      public Optional<ToolProvider> tool() {
        return Optional.of(this);
      }

      @Override
      public String name() {
        return "local";
      }

      @Override
      public List<Argument> arguments() {
        return arguments;
      }

      @Override
      public Local with(List<Argument> arguments) {
        return new Local(arguments);
      }

      @Override
      public int run(PrintWriter out, PrintWriter err, String... args) {
        out.printf("args -> %s", String.join("-", args));
        return 0;
      }
    }

    var zero = Project.of("zero", "0");
    var bach = Bach.of(zero).with(new Logbook(__ -> {}, Level.OFF));
    bach.call(new Local().with("1", 2, 3));

    assertLinesMatch(
        List.of(">>>>", "INFO.+local 1 2 3", "TRACE.+" + Pattern.quote("args -> 1-2-3"), "```"),
        bach.logbook().toMarkdown(zero));
  }
}
