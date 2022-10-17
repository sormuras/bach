package project;

import java.util.List;
import run.bach.Bach;
import run.bach.ToolOperator;

public final class build implements ToolOperator {
  @Override
  public void operate(Bach bach, List<String> arguments) {
    bach.run("project/format");
    bach.run("run.bach/build");
  }
}
