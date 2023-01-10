package run.bach.internal;

import run.duke.ToolCalls;
import run.duke.ToolLogger;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

public record ToolCallsToolOperator(ToolCalls calls) implements ToolOperator {
  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public int run(ToolRunner runner, ToolLogger logger, String... args) {
    for (var call : calls) runner.run(call);
    return 0;
  }
}
