package com.github.sormuras.bach;

/** Main entry-point and static helpers. */
public class Main {
  /**
   * Returns the version of Bach.
   *
   * @return the version as a string or {@code "?"} if the version is unknown at runtime
   */
  public static String help() {
    return """
        Usage: bach [options] action [actions/args...]
        Options:
          --verbose
                Output messages about what Bach and other tools are doing.
        Actions:
          build [args...]
                Build the modular Java project. Consumes all following arguments.
          clean
                Delete workspace directory.
          help, /?
                Print this help message.
          run TOOL [args...]
                Run the named tool with all following arguments being passed to it.
          tools
                Print a sorted list of all available tools.
          version
                Print Bach's version: %s
        """
        .formatted(Bach.version());
  }

  /** Hidden default constructor. */
  private Main() {}
}
