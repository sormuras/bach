package test.duke;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import jdk.jfr.Registered;
import run.duke.Tool;
import run.duke.ToolFinder;
import run.duke.ToolFinders;
import run.duke.ToolRunner;

@Registered
public class ToolFindersTests implements ToolProvider {
  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    testEmptyToolFinders(ToolFinders.EMPTY);
    testEmptyToolFinders(new ToolFinders());
    testExampleToolFinders();
    testMockToolFinder();
    return 0;
  }

  void testEmptyToolFinders(ToolFinders finders) {
    assert finders.list().size() == 0;
    assert finders.findTools().size() == 0;
    assert finders.findTool("tool").isEmpty();
    assert finders.findTool("example/tool").isEmpty();
  }

  void testExampleToolFinders() {
    var tool = new Tool.OfProvider("example/tool", new MockToolProvider("tool", 0));
    var finders = new ToolFinders().with(ToolFinder.ofTools("Examples", tool));
    assert finders.list().size() == 1;
    assert finders.findTools().size() == 1;
    assert finders.findTool("tool").isPresent();
    assert finders.findTool("example/tool").isPresent();
    assert finders.findTool("namespace/tool").isEmpty();
  }

  void testMockToolFinder() {
    var finders = new ToolFinders().with(new MockToolFinder(ToolRunner.nullRunner()));
    assert finders.list().size() == 1;
    assert finders.findTools().size() == 3 + 1;
    assert finders.findTool("mock0").isPresent();
    assert finders.findTool("mock1").isPresent();
    assert finders.findTool("mock2").isPresent();
    assert finders.findTool("moper").isPresent();
  }
}
