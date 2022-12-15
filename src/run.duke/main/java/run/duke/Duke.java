package run.duke;

import java.nio.file.Path;
import run.duke.tool.DukeTool;

public sealed interface Duke permits DukeTool {
  static ToolCall listTools() {
    return ToolCall.of("duke", "list", "tools");
  }

  static ToolCall treeDelete(Path start) {
    return ToolCall.of("duke", "tree", "--mode=DELETE", start);
  }
}
