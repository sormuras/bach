package run.duke;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

/** An operator is a tool provider that runs tool on a given workbench instance. */
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
   * @see #run(ToolRunner, PrintWriter, PrintWriter, String...)
   * @throws UnsupportedOperationException unconditionally
   */
  @Override
  default int run(PrintWriter out, PrintWriter err, String... args)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /**
   * Runs an instance of the tool, returning zero for a successful run.
   *
   * <p>Any non-zero return value indicates a tool-specific error during the execution.
   *
   * @param runner a finder and runner of tools
   * @param out a writer to which "expected" output should be written
   * @param err a writer to which any error messages should be written
   * @param args the command-line arguments for the tool
   * @return the result of executing the tool. A return value of 0 means the tool did not encounter
   *     any errors; any other value indicates that at least one error occurred during execution.
   */
  int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args);
}
