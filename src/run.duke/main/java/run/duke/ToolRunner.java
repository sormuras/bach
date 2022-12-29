package run.duke;

import java.util.List;
import java.util.stream.Stream;

@FunctionalInterface
public interface ToolRunner extends ToolFinder {
  default void run(String tool, Object... args) {
    run(ToolCall.of(tool).with(Stream.of(args)));
  }

  default void run(String tool, ToolCall.Tweak tweak) {
    run(ToolCall.of(tool).withTweak(tweak));
  }

  void run(ToolCall call);

  default List<Tool> tools() {
    return List.of();
  }
}
