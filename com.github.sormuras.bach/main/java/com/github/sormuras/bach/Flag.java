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
   * Mute all normal (expected) printouts.
   */
  SILENT,

  /**
   * Print Bach's version and exit.
   */
  VERSION,

  /**
   * Print Bach's version and continue.
   */
  SHOW_VERSION,

  /**
   * Print usage information and exit.
   */
  HELP,

  /**
   * Prevent parallel execution of commands.
   *
   * @see Bach#run(Command, Command[])
   * @see Bach#run(java.util.List)
   */
  RUN_COMMANDS_SEQUENTIALLY
}
