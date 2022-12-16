package run.duke;

import java.util.spi.ToolProvider;

@FunctionalInterface
public interface ToolOperator {
  /**
   * Returns the name of this tool operator.
   *
   * @apiNote It is recommended that the name be the same as would be used on the command line: for
   *     example, "javac", "jar", "jlink".
   * @return the name of this tool operator
   * @see ToolProvider#name()
   */
  default String name() {
    return getClass().getSimpleName();
  }

  ToolProvider provider(Workbench workbench);
}
