package run.bach;

import java.util.List;

@FunctionalInterface
public interface ToolOperator {
  void operate(Bach bach, List<String> arguments) throws Exception;

  default String name() {
    return getClass().getSimpleName();
  }
}
