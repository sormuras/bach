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

package de.sormuras.bach.execution;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.spi.ToolProvider;

/** Collection of tasks. */
public interface Tasks {

  /** Creates a directory by creating all nonexistent parent directories first. */
  class CreateDirectories extends Task {

    private final Path path;

    public CreateDirectories(Path path) {
      super("Create directories " + path, false, List.of());
      this.path = path;
    }

    @Override
    public ExecutionResult execute(ExecutionContext context) {
      try {
        Files.createDirectories(path);
        return context.ok();
      } catch (Exception e) {
        return context.failed(e);
      }
    }

    @Override
    public String toMarkdown() {
      return "`Files.createDirectories(Path.of(" + path + "))`";
    }
  }

  /** Tool-running task. */
  class RunToolProvider extends Task {

    /** Implement this marker interface indicating a {@link System#gc()} call is required. */
    public interface GarbageCollect {}

    private final ToolProvider[] tool;
    private final String[] args;
    private final String markdown;

    public RunToolProvider(String title, ToolProvider tool, String... args) {
      super(title, false, List.of());
      this.tool = new ToolProvider[] {tool};
      this.args = args;
      var arguments = args.length == 0 ? "" : ' ' + String.join(" ", args);
      this.markdown = '`' + tool.name() + arguments + '`';
    }

    @Override
    public ExecutionResult execute(ExecutionContext context) {
      var out = new StringWriter();
      var err = new StringWriter();
      var code = tool[0].run(new PrintWriter(out), new PrintWriter(err), args);
      var duration = Duration.between(context.start(), Instant.now());
      if (tool[0] instanceof GarbageCollect) {
        tool[0] = null;
        System.gc();
      }
      return new ExecutionResult(code, duration, out.toString(), err.toString(), null);
    }

    @Override
    public String toMarkdown() {
      return markdown;
    }
  }
}
