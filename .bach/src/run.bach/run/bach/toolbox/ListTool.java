package run.bach.toolbox;

import java.util.List;
import run.bach.Bach;
import run.bach.ToolOperator;

public record ListTool(String name) implements ToolOperator {
  public ListTool() {
    this("list");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    if (arguments.isEmpty()) {
      bach.info("Usage: %s {tools}".formatted(name()));
      return;
    }
    if (arguments.contains("tools")) {
      bach.info(bach.tools().toString(0));
    }
  }
}
