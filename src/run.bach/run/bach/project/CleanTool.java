package run.bach.project;

import run.bach.ToolOperator;
import run.bach.toolbox.TreeTool;

public record CleanTool(String name) implements ToolOperator {
  public CleanTool() {
    this("clean");
  }

  @Override
  public void run(Operation operation) throws Exception {
    var bach = operation.bach();
    var out = bach.paths().out();
    bach.run(TreeTool.clean(out));
  }
}
