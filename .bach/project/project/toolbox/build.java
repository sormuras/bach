package project.toolbox;

import run.bach.ToolOperator;

public final class build implements ToolOperator {
  @Override
  public void run(Operation operation) throws Exception {
    try {
      operation.run("project/format");
      operation.run("run.bach/build");
    } catch (Exception exception) {
      operation.bach().writeLogbook();
      throw exception;
    }
  }
}
