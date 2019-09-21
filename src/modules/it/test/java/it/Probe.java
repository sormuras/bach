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

package it;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Command;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

class Probe extends Bach {

  static class Run {
    final int code;
    final List<String> out;
    final List<String> err;

    Run(int code, List<String> out, List<String> err) {
      this.code = code;
      this.out = out;
      this.err = err;
    }
  }

  private static List<String> lines(StringWriter writer) {
    return List.copyOf(writer.toString().lines().collect(Collectors.toList()));
  }

  private final StringWriter out, err;

  Probe() {
    this(new StringWriter(), new StringWriter());
  }

  private Probe(StringWriter out, StringWriter err) {
    super(new PrintWriter(out, true), new PrintWriter(err, true), true);
    this.out = out;
    this.err = err;
  }

  List<String> lines() {
    return lines(out);
  }

  List<String> errors() {
    return lines(err);
  }

  Run run(Command command) {
    System.out.println(command.toCommandLine());
    var tool = ToolProvider.findFirst(command.getName()).orElseThrow();
    var out = new StringWriter();
    var err = new StringWriter();
    var args = command.toStringArray();
    var code = tool.run(new PrintWriter(out, true), new PrintWriter(err, true), args);
    return new Run(code, lines(out), lines(err));
  }
}
