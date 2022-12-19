package test.duke;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import jdk.jfr.Enabled;
import jdk.jfr.Registered;
import run.duke.ToolOperator;
import run.duke.Workbench;

@Registered
@Enabled
public class ToolOperatorTests implements ToolProvider {
  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var operator = new Operator();
    assert Operator.class.getSimpleName().equals(operator.name());
    assert null == operator.provider(null);
    return 0;
  }

  static final class Operator implements ToolOperator {
    @Override
    public ToolProvider provider(Workbench workbench) {
      return null;
    }
  }
}
