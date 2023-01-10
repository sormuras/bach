package run.bach.tool;

import run.bach.ProjectOperator;
import run.bach.ProjectRunner;
import run.duke.Duke;
import run.duke.ToolLogger;

public class CleanTool implements ProjectOperator {
  public CleanTool() {}

  @Override
  public final String name() {
    return "clean";
  }

  @Override
  public void run(ProjectRunner runner, ToolLogger logger, String... args) {
    runner.run(Duke.treeDelete(runner.folders().out()));
  }
}
