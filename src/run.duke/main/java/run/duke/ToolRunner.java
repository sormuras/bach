package run.duke;

import java.util.stream.Stream;

/** A finder and runner of tools that also provides context record instances. */
public interface ToolRunner extends ToolFinder, ToolContext {
  /**
   * Runs a tool with the given arguments.
   *
   * @param tool an identifier or nickname of the tool to run
   * @param args an array of arguments
   */
  default void run(String tool, Object... args) {
    run(ToolCall.of(tool).with(Stream.of(args)));
  }

  /**
   * Runs a tool with the given tool call tweak.
   *
   * @param tool an identifier or nickname of the tool to run
   * @param tweak an operator
   */
  default void run(String tool, ToolCall.Tweak tweak) {
    run(ToolCall.of(tool).withTweak(tweak));
  }

  /**
   * Runs a tool call.
   *
   * @param call a tool call to run
   */
  void run(ToolCall call);
}
