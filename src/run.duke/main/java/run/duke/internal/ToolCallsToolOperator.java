package run.duke.internal;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import run.duke.ToolCalls;
import run.duke.ToolOperator;
import run.duke.Workbench;

public record ToolCallsToolOperator(String name, ToolCalls calls) implements ToolOperator {
  @Override
  public ToolProvider provider(Workbench workbench) {
    return new Provider(name, workbench, calls);
  }

  record Provider(String name, Workbench workbench, ToolCalls calls) implements ToolProvider {
    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      for (var call : calls) workbench.run(call);
      return 0;
    }
  }
}
