package run.bach.internal;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import run.duke.ToolCalls;
import run.duke.ToolOperator;
import run.duke.Workbench;

public record ToolCallsToolOperator(ToolCalls calls) implements ToolOperator {
  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public ToolProvider provider(Workbench workbench) {
    return new Provider(workbench, calls);
  }

  record Provider(Workbench workbench, ToolCalls calls) implements ToolProvider {
    @Override
    public String name() {
      return getClass().getName();
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      for (var call : calls) workbench.run(call);
      return 0;
    }
  }
}
