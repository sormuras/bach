package run.duke;

import java.util.Collection;
import java.util.Optional;

@FunctionalInterface
public interface ToolFinder {
  Collection<Tool> tools();

  default Optional<Tool> findTool(String identifierOrNickname) {
    return tools().stream().filter(tool -> tool.test(identifierOrNickname)).findFirst();
  }
}
