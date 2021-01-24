package com.github.sormuras.bach;

/**
 * Feature toggle.
 */
public enum Flag {

  /**
   * Print messages about what Bach is doing.
   *
   * @see Bach#debug(String, Object...)
   */
  VERBOSE,

  /**
   * Prevent parallel execution of commands.
   *
   * @see Bach#run(Command, Command[])
   * @see Bach#run(java.util.List)
   */
  RUN_COMMANDS_SEQUENTIALLY
}
