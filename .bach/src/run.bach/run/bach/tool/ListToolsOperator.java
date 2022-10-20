package run.bach.tool;

import java.util.List;
import run.bach.Bach;
import run.bach.ToolOperator;

public record ListToolsOperator(String name) implements ToolOperator {
  public ListToolsOperator() {
    this("list-tools");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    bach.info(bach.tools().toString(0));
  }
}
