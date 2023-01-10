package run.duke;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

/** A tool operator is a tool provider running other tools. */
@FunctionalInterface
public interface ToolOperator extends ToolProvider {
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

  /**
   * Unconditionally throws an unsupported operation exception.
   *
   * @see #run(ToolRunner, ToolLogger, String...)
   */
  @Override
  @Deprecated
  default int run(PrintWriter out, PrintWriter err, String... args) {
    throw new UnsupportedOperationException();
  }

  /**
   * Runs an instance of the tool, returning zero for a successful run.
   *
   * <p>Any non-zero return value indicates a tool-specific error during the execution.
   *
   * @param runner a finder and runner of tools
   * @param logger a logger to which "expected" output and any error message should be written to
   * @param args the command-line arguments for the tool
   * @return the result of executing the tool. A return value of 0 means the tool did not encounter
   *     any errors; any other value indicates that at least one error occurred during execution.
   */
  int run(ToolRunner runner, ToolLogger logger, String... args);
}
