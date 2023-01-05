package test.duke;

import java.io.PrintWriter;
import jdk.jfr.Enabled;
import jdk.jfr.Registered;
import run.duke.ToolCall;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

@Registered
@Enabled
public class ToolOperatorTests implements ToolOperator, ToolRunner {
  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  @SuppressWarnings("deprecation")
  public int run(PrintWriter out, PrintWriter err, String... args) {
    return run(this, out, err, args);
  }

  @Override
  public int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
    assert runner == this;
    assert runner.workbench().get(Record.class) == null;
    assert runner.workpiece(Record.class) == null;
    return 0;
  }

  @Override
  public void run(ToolCall call) {}
}
