package test.duke;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import jdk.jfr.Enabled;
import jdk.jfr.Registered;
import run.duke.Toolbox;
import run.duke.Workpieces;

@Registered
@Enabled
public class WorkbenchTests implements ToolProvider {
  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    testCanonical();
    return 0;
  }

  void testCanonical() {
    var workpieces = new Workpieces().put(int.class, 123).put(char.class, '#');
    var workbench = new MockWorkbench(Toolbox.empty(), workpieces);
    assert 123 == workbench.workpiece(int.class);
    assert '#' == workbench.workpiece(char.class);
    assert null == workbench.workpiece(boolean.class);
  }
}
