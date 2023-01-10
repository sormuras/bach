package project;

import run.bach.ProjectRunner;
import run.bach.tool.BuildTool;
import run.duke.ToolLogger;

public final class Build extends BuildTool {
  @Override
  public void run(ProjectRunner runner, ToolLogger logger, String... args) {
    try {
      logger.log("BEGIN");
      super.run(runner, logger, args);
    } catch (Exception exception) {
      logger.error("Build failed", exception);
    } finally {
      logger.log("END.");
    }
  }
}
