package com.github.sormuras.bach;

import java.util.List;
import java.util.stream.Stream;

public interface ToolRunner {
  /** Run the given tool call. */
  default void run(ToolCall call) {
    run(call.name(), call.arguments());
  }

  default void run(String command) {
    run(ToolCall.of(command.lines().toList()));
  }

  /** Run the tool specified the given name and passing the given arguments. */
  default void run(String name, Object... arguments) {
    run(name, Stream.of(arguments).map(Object::toString).toList());
  }

  /** Run the tool specified by its name and passing the given arguments. */
  void run(String name, List<String> arguments);

  /** Run the tool specified by its name and passing the given arguments. */
  void run(ToolFinder finder, String name, List<String> arguments);
}
