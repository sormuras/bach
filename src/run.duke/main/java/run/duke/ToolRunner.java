package run.duke;

import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface ToolRunner {
  void run(ToolCall call);

  default Toolbox toolbox() {
    return Toolbox.EMPTY;
  }

  default void run(String name, String... args) {
    run(new ToolCall(name, List.of(args)));
  }
}
