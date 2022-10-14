package run.bach.internal.tool;

import java.util.List;
import run.bach.Bach;
import run.bach.ToolOperator;

public record ListPathsOperator(String name) implements ToolOperator {
  public ListPathsOperator() {
    this("list-paths");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    bach.info(bach.paths().toString(0));
  }
}
