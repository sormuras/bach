package run.duke;

import java.util.Optional;
import java.util.stream.Stream;

public interface Workbench {
  Optional<Tool> find(String tool);

  default void run(String tool, Object... args) {
    run(ToolCall.of(tool).with(Stream.of(args)));
  }

  default void run(String tool, ToolCall.Tweak tweak) {
    run(ToolCall.of(tool).withTweak(tweak));
  }

  void run(ToolCall call);

  <T> T workpiece(Class<T> type);
}
