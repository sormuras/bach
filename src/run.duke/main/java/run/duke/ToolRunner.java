package run.duke;

import java.util.List;

@FunctionalInterface
public interface ToolRunner {
  default ToolFinders finders() {
    return ToolFinders.EMPTY;
  }

  void run(ToolCall call);

  default void run(String tool, String... args) {
    run(new ToolCall(tool, List.of(args)));
  }

  default void run(String tool, ToolCall.Tweak composer) {
    run(new ToolCall(tool).withTweak(composer));
  }
}
