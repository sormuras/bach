package run.duke;

import java.util.List;
import java.util.spi.ToolProvider;
import run.duke.internal.NullToolRunner;

@FunctionalInterface
public interface ToolRunner {
  static ToolRunner nullRunner() {
    return NullToolRunner.INSTANCE;
  }

  default void run(String tool, String... args) {
    run(new ToolCall(tool, List.of(args)));
  }

  default void run(String tool, ToolCall.Tweak composer) {
    run(new ToolCall(tool).withTweak(composer));
  }

  default void run(ToolCall call) {
    var tool = toolFinders().findTool(call.name()).orElseThrow();
    var provider = toolProvider(tool);
    var args = call.arguments().toArray(String[]::new);
    run(provider, args);
  }

  void run(ToolProvider provider, String... args);

  default ToolFinders toolFinders() {
    return ToolFinders.EMPTY;
  }

  default ToolProvider toolProvider(Tool tool) {
    if (tool instanceof Tool.OfProvider of) return of.provider();
    if (tool instanceof Tool.OfOperator of) return of.operator().provider(this);
    throw new RuntimeException("Unsupported tool of " + tool.getClass());
  }
}
