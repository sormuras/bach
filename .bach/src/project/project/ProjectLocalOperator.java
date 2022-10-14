package project;

import java.util.List;
import run.bach.Bach;
import run.bach.ToolOperator;

public class ProjectLocalOperator implements ToolOperator {
  @Override
  public String name() {
    return "noop";
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {}
}
