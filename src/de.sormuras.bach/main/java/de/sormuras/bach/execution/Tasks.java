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
import java.util.Set;
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
    public Snippet toSnippet() {
      return new Snippet(
          Set.of(Files.class, Path.class),
          List.of(String.format("Files.createDirectories(%s);", $(path))));
    }
  }

  /** Tool-running task. */
  class RunToolProvider extends Task {

    /** Implement this marker interface indicating a {@link System#gc()} call is required. */
    public interface GarbageCollect {}

    static String title(String tool, String... args) {
      var length = args.length;
      if (length == 0) return String.format("Run `%s`", tool);
      if (length == 1) return String.format("Run `%s %s`", tool, args[0]);
      if (length == 2) return String.format("Run `%s %s %s`", tool, args[0], args[1]);
      return String.format("Run `%s %s %s ...` (%d arguments)", tool, args[0], args[1], length);
    }

    private final ToolProvider[] tool;
    private final String name;
    private final String[] args;

    public RunToolProvider(ToolProvider tool, String... args) {
      super(title(tool.name(), args), false, List.of());
      this.tool = new ToolProvider[] {tool};
      this.name = tool.name();
      this.args = args;
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
    public Snippet toSnippet() {
      return Snippet.of(String.format("run(%s, %s);", $(name), $(args)));
    }
  }
}
