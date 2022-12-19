package test.duke;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import run.duke.Tooling;
import run.duke.Workbench;

public record MockTooling(Workbench workbench) implements Tooling {
  public MockTooling() {
    this(Workbench.inoperative());
  }

  @Override
  public String name() {
    return "mock99";
  }

  @Override
  public ToolProvider provider(Workbench workbench) {
    return new MockTooling(workbench);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    return 99;
  }
}
