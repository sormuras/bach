package test.bach;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import jdk.jfr.Registered;
import run.duke.Tool;
import run.duke.ToolCall;
import run.duke.ToolRunner;
import run.duke.Toolbox;

@Registered
public class ToolboxTests implements ToolProvider {
  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    testEmptyToolbox(Toolbox.EMPTY);
    testEmptyToolbox(new Toolbox());
    testExampleToolbox();

    return 0;
  }

  void testEmptyToolbox(Toolbox toolbox) {
    var runner = new DryRunner();
    assert toolbox.size() == 0;
    assert toolbox.find("tool", runner).isEmpty();
    assert toolbox.find("example/tool", runner).isEmpty();
  }

  void testExampleToolbox() {
    var tool = new Tool("example", "tool", new MockToolProvider("tool", 0));
    var toolbox = new Toolbox().with("Examples", tool);
    var runner = new DryRunner();
    assert toolbox.size() == 1;
    assert toolbox.find("tool", runner).isPresent();
    assert toolbox.find("example/tool", runner).isPresent();
    assert toolbox.find("namespace/tool", runner).isEmpty();
  }

  record DryRunner() implements ToolRunner {
    @Override
    public void run(ToolCall call) {}
  }
}
