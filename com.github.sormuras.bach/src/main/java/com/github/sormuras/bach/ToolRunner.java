package com.github.sormuras.bach;

import java.util.List;
import java.util.Set;

public interface ToolRunner {

  enum RunModifier {
    HIDDEN,
    RUN_WITH_PROVIDERS_CLASS_LOADER
  }

  default void run(String command) {
    run(ToolCall.of(command.lines().toList()));
  }

  /** Run the tool specified the given name and passing the given arguments. */
  default void run(String name, Object... arguments) {
    run(ToolCall.of(name, arguments));
  }

  /** Run the tool specified by its name and passing the given arguments. */
  default void run(String name, List<String> arguments) {
    run(new ToolCall(name, arguments));
  }

  /** Run the given tool call using a default tool finder. */
  default void run(ToolCall call, RunModifier... modifiers) {
    run(call, Set.of(modifiers));
  }

  void run(ToolCall call, Set<RunModifier> modifiers);

  default void run(ToolFinder finder, ToolCall call, RunModifier... modifiers) {
    run(finder, call, Set.of(modifiers));
  }

  /** Run the given tool call using the given tool finder. */
  void run(ToolFinder finder, ToolCall call, Set<RunModifier> modifiers);
}
