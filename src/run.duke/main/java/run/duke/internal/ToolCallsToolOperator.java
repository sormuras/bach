package run.duke.internal;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import run.duke.ToolCalls;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

public record ToolCallsToolOperator(String name, ToolCalls calls) implements ToolOperator {
  @Override
  public ToolProvider provider(ToolRunner runner) {
    return new Provider(name, runner, calls);
  }

  record Provider(String name, ToolRunner runner, ToolCalls calls) implements ToolProvider {
    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      for (var call : calls) runner.run(call);
      return 0;
    }
  }
}
