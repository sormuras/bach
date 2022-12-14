package run.duke;

import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface ToolTrait {
  ToolRunner toolRunner();

  default Optional<Tool> find(String tool) {
    return toolRunner().toolFinders().findTool(tool);
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
    var runner = toolRunner();
    var provider = Tool.provider(tool, runner);
    var args = call.arguments().toArray(String[]::new);
    runner.run(provider, args);
  }
}
