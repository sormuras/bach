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

package de.sormuras.bach.execution.task;

import de.sormuras.bach.execution.ExecutionContext;
import de.sormuras.bach.execution.ExecutionResult;
import de.sormuras.bach.execution.Scribe;
import de.sormuras.bach.execution.Snippet;
import de.sormuras.bach.execution.Task;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.spi.ToolProvider;

/** Tool-running task. */
public /*static*/ class RunToolProvider extends Task {

  static String title(String tool, String... args) {
    var length = args.length;
    if (length == 0) return String.format("Run `%s`", tool);
    if (length == 1) return String.format("Run `%s %s`", tool, args[0]);
    if (length == 2) return String.format("Run `%s %s %s`", tool, args[0], args[1]);
    return String.format("Run `%s %s %s ...` (%d arguments)", tool, args[0], args[1], length);
  }

  private final ToolProvider tool;
  private final String[] args;
  private final Snippet snippet;

  public RunToolProvider(ToolProvider tool, String... args) {
    super(title(tool.name(), args), false, List.of());
    this.tool = tool;
    this.args = args;
    var empty = args.length == 0;
    this.snippet =
        tool instanceof Scribe
            ? ((Scribe) tool).toSnippet()
            : Snippet.of("run(" + $(tool.name()) + (empty ? "" : ", " + $(args)) + ");");
  }

  @Override
  public ExecutionResult execute(ExecutionContext context) {
    var out = new StringWriter();
    var err = new StringWriter();
    var code = tool.run(new PrintWriter(out), new PrintWriter(err), args);
    var duration = Duration.between(context.start(), Instant.now());
    return new ExecutionResult(code, duration, out.toString(), err.toString(), null);
  }

  @Override
  public Snippet toSnippet() {
    return snippet;
  }
}
