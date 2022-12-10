package test.duke;

import java.util.List;
import java.util.Optional;
import run.duke.Tool;
import run.duke.ToolFinder;
import run.duke.ToolRunner;

public record MockToolFinder() implements ToolFinder {
  @Override
  public List<String> identifiers(ToolRunner runner) {
    return List.of("test.duke/mock0", "test.duke/mock1", "test.duke/mock2", "test.duke/moper");
  }

  @Override
  public Optional<Tool> find(String name, ToolRunner runner) {
    return Optional.ofNullable(
        switch (name) {
          case "mock0", "test.duke/mock0" -> new Tool(new MockToolProvider("mock" + 0, 0));
          case "mock1", "test.duke/mock1" -> new Tool(new MockToolProvider("mock" + 1, 1));
          case "mock2", "test.duke/mock2" -> new Tool(new MockToolProvider("mock" + 2, 2));
          case "moper", "test.duke/moper" -> new Tool(new MockToolOperator("moper", runner));
          default -> null;
        });
  }
}
