package run.bach;

import java.util.List;

@FunctionalInterface
public interface ToolOperator {
  void run(Operation operation) throws Exception;

  default String name() {
    return getClass().getSimpleName();
  }

  record Operation(Bach bach, List<String> arguments) implements ToolRunner {
    @Override
    public void run(ToolCall call) {
      bach.run(call);
    }
  }
}
