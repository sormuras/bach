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
    assert toolbox.find("tool").isEmpty();
    assert toolbox.find("example/tool").isEmpty();
  }

  void testExampleToolbox() {
    var tool = Tool.of("example/tool", new MockToolProvider("tool", 0));
    var toolbox = Toolbox.of(tool);
    assert toolbox.tools().size() == 1;
    assert toolbox.find("tool").isPresent();
    assert toolbox.find("example/tool").isPresent();
    assert toolbox.find("namespace/tool").isEmpty();
  }

  void testMockToolbox() {
    var toolbox = new MockToolbox();
    assert toolbox.tools().size() == 3 + 1;
    assert toolbox.find("mock0").isPresent();
    assert toolbox.find("mock1").isPresent();
    assert toolbox.find("mock2").isPresent();
    assert toolbox.find("moper").isPresent();
  }

  void testSystemToolbox() {
    var toolbox = Toolbox.ofModuleLayer(ModuleLayer.boot());
    assert toolbox.find("duke").isPresent();
    assert toolbox.find("jar").isPresent();
    assert toolbox.find("javac").isPresent();
    assert toolbox.find("run.duke/duke").isPresent();
  }
}
