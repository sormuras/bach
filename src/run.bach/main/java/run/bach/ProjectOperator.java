package run.bach;

import run.duke.ToolLogger;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

public interface ProjectOperator extends ToolOperator {
  @Override
  default int run(ToolRunner runner, ToolLogger logger, String... args) {
    run((ProjectRunner) runner, logger, args);
    return 0;
  }

  void run(ProjectRunner runner, ToolLogger logger, String... args);
}
