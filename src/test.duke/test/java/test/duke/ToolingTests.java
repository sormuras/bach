package test.duke;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import jdk.jfr.Enabled;
import jdk.jfr.Registered;
import run.duke.ToolCall;
import run.duke.Workbench;

@Registered
@Enabled
public class ToolingTests implements ToolProvider {
  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var tooling = new MockTooling();
    assert "mock99".equals(tooling.name());
    assert "mock99".equals(tooling.provider(Workbench.inoperative()).name());
    assert 99 == tooling.run(System.out, System.err);
    assert tooling.find("tool").isEmpty();
    tooling.run("tool");
    tooling.run("tool", tool -> tool.with("..."));
    tooling.run(ToolCall.of("tool"));
    return 0;
  }
}
