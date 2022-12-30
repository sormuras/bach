package run.duke;

import java.lang.invoke.MethodHandles.Lookup;
import java.nio.file.Path;
import run.duke.tool.DukeTool;

public sealed interface Duke permits DukeTool {
  static <R extends Record> R split(Lookup lookup, Class<R> schema, String... args) {
    return splitter(lookup, schema).split(args);
  }

  static <R extends Record> CommandLineInterface<R> splitter(Lookup lookup, Class<R> schema) {
    return CommandLineInterface.of(lookup, schema);
  }

  static ToolCall listTools() {
    return ToolCall.of("duke", "list", "tools");
  }

  static ToolCall treeDelete(Path start) {
    return ToolCall.of("duke", "tree", "--mode=DELETE", start);
  }
}
