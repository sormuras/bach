package run.bach;

import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface ToolFinder {
  default String description() {
    return getClass().getSimpleName();
  }

  Optional<Tool> find(String tool, ToolRunner runner);

  default List<String> identifiers() {
    return List.of();
  }
}
