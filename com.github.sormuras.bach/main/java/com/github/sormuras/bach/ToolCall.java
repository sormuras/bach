package com.github.sormuras.bach;

/** A tool call object with its name and an array of argument strings. */
public interface ToolCall {
  /**
   * Return the name of the tool to be called.
   *
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
