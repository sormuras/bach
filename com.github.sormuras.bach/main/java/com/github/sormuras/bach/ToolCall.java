package com.github.sormuras.bach;

/** A tool call object with its name and an array of argument strings. */
public interface ToolCall {
  /**
   * Return the name of the tool to be called.
   *
   * @apiNote It is recommended that the name be the same as would be used on the command line: for
   *     example, {@code "javac"}, {@code "jar"}, {@code "jlink"}.
   * @return a non-empty string representing the name of the tool to be called
   */
  String name();

  /**
   * Return the possibly empty arguments of this tool call.
   *
   * @return an array of strings
   */
  String[] args();
}
