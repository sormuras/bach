package test.duke;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import jdk.jfr.Enabled;
import jdk.jfr.Registered;
import run.duke.Workpieces;

@Registered
@Enabled
public class WorkpiecesTests implements ToolProvider {
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
    assert 123 == workpieces.get(int.class);
    assert '#' == workpieces.get(char.class);
    assert null == workpieces.get(boolean.class);
  }
}
