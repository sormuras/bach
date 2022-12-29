package run.duke;

import java.util.Optional;
import java.util.stream.Stream;
import run.duke.internal.InoperativeWorkbench;

public interface Workbench {
  static Workbench inoperative() {
    return new InoperativeWorkbench();
  }

  default Optional<Tool> find(String tool) {
    return toolbox().find(tool);
  }

  default void run(String tool, Object... args) {
    run(ToolCall.of(tool).with(Stream.of(args)));
  }

  default void run(String tool, ToolCall.Tweak tweak) {
    run(ToolCall.of(tool).withTweak(tweak));
  }

  void run(ToolCall call);

  default Toolbox toolbox() {
    return workpiece(Toolbox.class);
  }

  <T> T workpiece(Class<T> type);
}
