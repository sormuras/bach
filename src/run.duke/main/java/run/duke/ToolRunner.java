package run.duke;

import java.util.List;

@FunctionalInterface
public interface ToolRunner {
  void run(ToolCall call);

  default ToolFinders finders() {
    return ToolFinders.EMPTY;
  }

  default void run(String name, String... args) {
    run(new ToolCall(name, List.of(args)));
  }
}
