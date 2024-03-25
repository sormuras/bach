/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

import java.util.function.UnaryOperator;

/**
 * A runner of tool calls.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * ToolRunner.ofSystem().run("java", "--version");
 * }</pre>
 */
@FunctionalInterface
public interface ToolRunner {
  static ToolRunner of(ToolFinder finder) {
    return new Workbench(finder);
  }

  /** {@return an instance of the default tool runner implementation using the system tool finder} */
  static ToolRunner ofSystem() {
    class SystemRunner {
      static final ToolRunner SINGLETON = ToolRunner.of(ToolFinder.ofSystem());
    }
    return SystemRunner.SINGLETON;
  }

  ToolRun run(ToolCall call);

  default ToolRun run(Tool tool, String... args) {
    return run(ToolCall.of(tool).addAll(args));
  }

  default ToolRun run(Tool tool, UnaryOperator<ToolCall> args) {
    return run(args.apply(ToolCall.of(tool)));
  }

  default ToolRun run(String tool, String... args) {
    return run(ToolCall.of(tool).addAll(args));
  }

  default ToolRun run(String tool, UnaryOperator<ToolCall> args) {
    return run(args.apply(ToolCall.of(tool)));
  }
}
