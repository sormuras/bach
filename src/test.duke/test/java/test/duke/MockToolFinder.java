package test.duke;

import java.util.List;
import run.duke.Tool;
import run.duke.ToolFinder;

public record MockToolFinder() implements ToolFinder {
  @Override
  public List<Tool> tools() {
    return List.of(
        Tool.of(new MockToolProvider("mock" + 0, 0)),
        Tool.of(new MockToolProvider("mock" + 1, 1)),
        Tool.of(new MockToolProvider("mock" + 2, 2)),
        Tool.of(new MockToolOperator()));
  }
}
