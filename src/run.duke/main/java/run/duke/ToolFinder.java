package run.duke;

import java.util.List;
import java.util.Optional;

/** A finder of tools. */
@FunctionalInterface
public interface ToolFinder {
  /** {@return a list of runnable tool instances} */
  List<Tool> tools();

  /**
   * {@return the first tool matching the query wrapped in an optional, or empty optional}
   *
   * @param identifierOrNickname the query string, typically an identifier or a nickname of a tool
   */
  default Optional<Tool> findTool(String identifierOrNickname) {
    return tools().stream().filter(tool -> tool.test(identifierOrNickname)).findFirst();
  }
}
