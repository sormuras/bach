package project;

import run.bach.ToolOperator;

public final class build implements ToolOperator {
  @Override
  public void run(Operation operation) {
    operation.run("project/format");
    operation.run("run.bach/build");
  }
}
