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

package de.sormuras.bach.task;

import de.sormuras.bach.Convention;
import de.sormuras.bach.Task;
import de.sormuras.bach.Tool;
import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/** {@link ToolProvider}-running task. */
public /*static*/ class RunTool extends Task {

  @Convention
  static String name(String tool, String... args) {
    var length = args.length;
    if (length == 0) return String.format("Run %s", tool);
    if (length == 1) return String.format("Run %s %s", tool, args[0]);
    if (length == 2) return String.format("Run %s %s %s", tool, args[0], args[1]);
    return String.format("Run %s %s %s ... (%d arguments)", tool, args[0], args[1], length);
  }

  private final ToolProvider tool;
  private final String[] args;

  public RunTool(ToolProvider tool, String... args) {
    super(name(tool.name(), args));
    this.tool = tool;
    this.args = args;
  }

  @Override
  public void execute(Execution execution) {
    var out = execution.getOut();
    var err = execution.getErr();
    var code = tool.run(new PrintWriter(out), new PrintWriter(err), args);

    var LS = System.lineSeparator();
    var indented = Collectors.joining(LS + "\t");
    execution.print(Level.DEBUG, "\t" + out.toString().lines().collect(indented));
    if (code != 0) {
      var name = tool.name();
      var caption = "Run of " + name + " failed with exit code: " + code;
      var error = "\t" + err.toString().lines().collect(indented);
      var lines = "\t" + Tool.toStrings(name, args).stream().collect(indented);
      var message = String.join(LS, caption, "Error:", error, "Tool:", lines);
      throw new AssertionError(message);
    }
  }
}
