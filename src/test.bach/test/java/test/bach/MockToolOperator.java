package test.bach;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import run.duke.ToolRunner;

record MockToolOperator(String name, ToolRunner runner) implements ToolProvider {
  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    for (var arg : args) runner.run(arg);
    return 0;
  }
}
