package test.duke;

import java.io.PrintWriter;
import jdk.jfr.Enabled;
import jdk.jfr.Registered;
import run.duke.ToolOperator;
import run.duke.Workbench;

@Registered
@Enabled
public class ToolOperatorTests implements ToolOperator {
  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public int run(Workbench workbench, PrintWriter out, PrintWriter err, String... args) {
    return 0;
  }
}
