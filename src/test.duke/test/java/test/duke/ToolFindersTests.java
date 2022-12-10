package test.duke;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import jdk.jfr.Registered;
import run.duke.Tool;
import run.duke.ToolCall;
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

    return 0;
  }

  void testEmptyToolFinders(ToolFinders finders) {
    var runner = new DryRunner();
    assert finders.list().size() == 0;
    assert finders.identifiers(runner).size() == 0;
    assert finders.find("tool", runner).isEmpty();
    assert finders.find("example/tool", runner).isEmpty();
  }

  void testExampleToolFinders() {
    var tool = new Tool("example", "tool", new MockToolProvider("tool", 0));
    var finders = new ToolFinders().with(ToolFinder.ofTools("Examples", tool));
    var runner = new DryRunner();
    assert finders.list().size() == 1 : finders.identifiers(runner).size();
    assert finders.identifiers(runner).size() == 1 : finders.identifiers(runner).size();
    assert finders.find("tool", runner).isPresent();
    assert finders.find("example/tool", runner).isPresent();
    assert finders.find("namespace/tool", runner).isEmpty();
  }

  record DryRunner() implements ToolRunner {
    @Override
    public void run(ToolCall call) {}
  }
}
