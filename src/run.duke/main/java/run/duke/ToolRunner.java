package run.duke;

import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

@FunctionalInterface
public interface ToolRunner {
  Workbench workbench();

  default void run(String tool, Object... args) {
    workbench().run(ToolCall.of(tool).with(Stream.of(args)));
  }

  default void run(String tool, ToolCall.Tweak tweak) {
    workbench().run(ToolCall.of(tool).withTweak(tweak));
  }

  default void run(ToolCall call) {
    workbench().run(call);
  }

  default void run(ToolProvider provider, List<String> arguments) {
    workbench().run(provider, arguments);
  }
}
