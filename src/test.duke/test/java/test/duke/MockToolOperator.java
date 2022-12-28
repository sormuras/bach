package test.duke;

import java.io.PrintWriter;
import run.duke.ToolOperator;
import run.duke.Workbench;

record MockToolOperator() implements ToolOperator {
  @Override
  public String name() {
    return "moper";
  }

  @Override
  public int run(Workbench workbench, PrintWriter out, PrintWriter err, String... args) {
    for (var arg : args) workbench.run(arg);
    return 0;
  }
}
