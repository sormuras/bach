package run.bach.toolbox;

import run.bach.ToolOperator;

public record ListTool(String name) implements ToolOperator {
  public ListTool() {
    this("list");
  }

  @Override
  public void run(Operation operation) {
    var bach = operation.bach();
    if (operation.arguments().isEmpty()) {
      bach.info("Usage: %s {tools}".formatted(name()));
      return;
    }
    if (operation.arguments().contains("tools")) {
      bach.info(bach.tools().toString(0));
    }
  }
}
