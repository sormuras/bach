package run.bach.internal;

import java.io.PrintWriter;
import run.duke.ToolCalls;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

public record ToolCallsToolOperator(ToolCalls calls) implements ToolOperator {
  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
    for (var call : calls) runner.run(call);
    return 0;
  }
}
