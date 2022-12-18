package run.duke;

import java.util.spi.ToolProvider;

/** An operator creates tool providers for a given workbench instance. */
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

  /** {@return a workbench-consuming tool provider instance} */
  ToolProvider provider(Workbench workbench);
}
