package test.duke;

import java.io.PrintWriter;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

record MockToolOperator() implements ToolOperator {
  @Override
  public String name() {
    return "moper";
  }

  @Override
  public int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
    for (var arg : args) runner.run(arg);
    return 0;
  }
}
