package run.duke;

import java.util.List;
import java.util.Optional;
import java.util.spi.ToolProvider;

@FunctionalInterface
public interface ToolRunner {
  Workbench workbench();

  default Optional<Tool> find(String tool) {
    return workbench().toolbox().find(tool);
  }

  default void run(String tool, String... args) {
    run(new ToolCall(tool, List.of(args)));
  }

  default void run(String tool, ToolCall.Tweak tweak) {
    run(new ToolCall(tool).withTweak(tweak));
  }

  default void run(ToolCall call) {
    var name = call.name();
    var tool = find(name).orElseThrow(() -> new ToolNotFoundException(name));
    var args = call.arguments().toArray(String[]::new);
    var workbench = workbench();
    var provider = switchOverToolAndYieldToolProvider(tool, workbench);
    workbench.run(call, provider, args);
  }

  private static ToolProvider switchOverToolAndYieldToolProvider(Tool tool, Workbench workbench) {
    if (tool instanceof Tool.OfProvider of) return of.provider();
    if (tool instanceof Tool.OfOperator of) return of.operator().provider(workbench);
    throw new Error("Unsupported tool of " + tool.getClass());
  }
}
