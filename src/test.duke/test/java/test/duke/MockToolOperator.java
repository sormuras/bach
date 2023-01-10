package test.duke;

import run.duke.ToolLogger;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

record MockToolOperator() implements ToolOperator {
  @Override
  public String name() {
    return "moper";
  }

  @Override
  public int run(ToolRunner runner, ToolLogger logger, String... args) {
    for (var arg : args) runner.run(arg);
    return 0;
  }
}
