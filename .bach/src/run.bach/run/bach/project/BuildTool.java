package run.bach.project;

import run.bach.ToolOperator;

public class BuildTool implements ToolOperator {
  public BuildTool() {}

  @Override
  public String name() {
    return "build";
  }

  @Override
  public void run(Operation operation) {
    operation.run(CacheTool.class); // go offline and verify cached assets
    operation.run(CompileTool.class); // compile all modules spaces
    operation.run(TestTool.class); // start launcher and execute tests in test space
  }
}
