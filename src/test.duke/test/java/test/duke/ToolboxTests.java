package test.duke;

import java.io.PrintWriter;
import java.util.List;
import java.util.spi.ToolProvider;
import jdk.jfr.Registered;
import run.duke.Tool;
import run.duke.Toolbox;

@Registered
public class ToolboxTests implements ToolProvider {
  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    testEmptyToolbox(Toolbox.empty());
    testEmptyToolbox(Toolbox.compose());
    testEmptyToolbox(List::of);
    testExampleToolbox();
    testMockToolbox();
    testSystemToolbox();
    return 0;
  }

  void testEmptyToolbox(Toolbox toolbox) {
    assert toolbox.tools().size() == 0;
    assert toolbox.findTool("tool").isEmpty();
    assert toolbox.findTool("example/tool").isEmpty();
  }

  void testExampleToolbox() {
    var tool = Tool.of("example/tool", new MockToolProvider("tool", 0));
    var toolbox = Toolbox.of(tool);
    assert toolbox.tools().size() == 1;
    assert toolbox.findTool("tool").isPresent();
    assert toolbox.findTool("example/tool").isPresent();
    assert toolbox.findTool("namespace/tool").isEmpty();
  }

  void testMockToolbox() {
    var toolbox = new MockToolbox();
    assert toolbox.tools().size() == 3 + 1;
    assert toolbox.findTool("mock0").isPresent();
    assert toolbox.findTool("mock1").isPresent();
    assert toolbox.findTool("mock2").isPresent();
    assert toolbox.findTool("moper").isPresent();
  }

  void testSystemToolbox() {
    var toolbox = Toolbox.ofModuleLayer(ModuleLayer.boot());
    assert toolbox.findTool("duke").isPresent();
    assert toolbox.findTool("jar").isPresent();
    assert toolbox.findTool("javac").isPresent();
    assert toolbox.findTool("run.duke/duke").isPresent();
  }
}
