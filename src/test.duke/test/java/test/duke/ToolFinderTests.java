package test.duke;

import java.io.PrintWriter;
import java.util.List;
import java.util.spi.ToolProvider;
import jdk.jfr.Registered;
import run.duke.Tool;
import run.duke.ToolFinder;

@Registered
public class ToolFinderTests implements ToolProvider {
  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    testEmptyToolFinder(ToolFinder.empty());
    testEmptyToolFinder(ToolFinder.compose());
    testEmptyToolFinder(List::of);
    testExampleToolFinder();
    testMockToolFinder();
    testSystemToolFinder();
    return 0;
  }

  void testEmptyToolFinder(ToolFinder finder) {
    assert finder.tools().size() == 0;
    assert finder.findTool("tool").isEmpty();
    assert finder.findTool("example/tool").isEmpty();
  }

  void testExampleToolFinder() {
    var tool = Tool.of("example/tool", new MockToolProvider("tool", 0));
    var finder = ToolFinder.of(tool);
    assert finder.tools().size() == 1;
    assert finder.findTool("tool").isPresent();
    assert finder.findTool("example/tool").isPresent();
    assert finder.findTool("namespace/tool").isEmpty();
  }

  void testMockToolFinder() {
    var finder = new MockToolFinder();
    assert finder.tools().size() == 3 + 1;
    assert finder.findTool("mock0").isPresent();
    assert finder.findTool("mock1").isPresent();
    assert finder.findTool("mock2").isPresent();
    assert finder.findTool("moper").isPresent();
  }

  void testSystemToolFinder() {
    var finder = ToolFinder.ofModuleLayer(ModuleLayer.boot());
    assert finder.findTool("duke").isPresent();
    assert finder.findTool("jar").isPresent();
    assert finder.findTool("javac").isPresent();
    assert finder.findTool("run.duke/duke").isPresent();
  }
}
