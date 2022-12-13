package test.duke;

import java.util.List;
import run.duke.Tool;
import run.duke.ToolFinder;
import run.duke.ToolRunner;

public record MockToolFinder(ToolRunner runner) implements ToolFinder {
  @Override
  public List<Tool> findTools() {
    return List.of(
        Tool.of(new MockToolProvider("mock" + 0, 0)),
        Tool.of(new MockToolProvider("mock" + 1, 1)),
        Tool.of(new MockToolProvider("mock" + 2, 2)),
        Tool.of(new MockToolOperator("moper", runner)));
  }
}
