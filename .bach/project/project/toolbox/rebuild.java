package project.toolbox;

import run.bach.ToolOperator;

public final class rebuild implements ToolOperator {
  @Override
  public void run(Operation operation) {
    operation.run("clean");
    operation.run("build");
  }
}
