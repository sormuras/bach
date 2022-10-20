package run.bach;

import java.util.List;

@FunctionalInterface
public interface ToolRunner {
  void run(ToolCall call);

  default void run(String tool, String... args) {
    run(new ToolCall(tool, List.of(args)));
  }

  default void run(String tool, List<String> arguments) {
    run(new ToolCall(tool, List.copyOf(arguments)));
  }

  default void run(String tool, ToolTweak composer) {
    run(new ToolCall(tool).withTweaks(List.of(composer)));
  }
}
