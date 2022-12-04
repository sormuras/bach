package run.duke;

import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface ToolFinder {
  /** {@return a description of the finder, or an empty {@code String} if none is available} */
  default String description() {
    return getClass().getSimpleName();
  }

  /**
   * Find a tool by its identifier or its nickname.
   *
   * @param identifier the identifier or the short variant of the tool to look for
   * @param runner the tool runner context; sometimes required to create a tool instance
   * @return a tool instance wrapped in an optional, or an empty optional wrapper
   */
  Optional<Tool> find(String identifier, ToolRunner runner);

  /** {@return a possibly empty list of tool identifying-strings} */
  default List<String> identifiers() {
    return List.of();
  }
}
