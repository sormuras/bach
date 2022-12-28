package run.duke;

import java.util.stream.Stream;

@FunctionalInterface
public interface ToolRunner {
  Workbench workbench();

  default void run(String tool, Object... args) {
    run(ToolCall.of(tool).with(Stream.of(args)));
  }

  default void run(String tool, ToolCall.Tweak tweak) {
    run(ToolCall.of(tool).withTweak(tweak));
  }

  default void run(ToolCall call) {
    workbench().run(call);
  }
}
