package com.github.sormuras.bach;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

/** Bach's main program. */
public class Main implements ToolProvider {

  public static void main(String... args) {
    System.exit(new Main().run(args));
  }

  public static void help(PrintWriter printer) {
    printer.print(
        """
        Usage: bach [OPTIONS] ACTION [ACTIONS...]
                 to execute one or more actions in sequence
           or: bach [OPTIONS] [ACTIONS...] tool NAME [ARGS...]
                 to execute a provided tool with tool-specific arguments

        Options include:

          --version
            Print version information and exit.
          --show-version
            Print version information and continue.

        Actions include:

          build
            Build the current project.
          clean
            Delete workspace directory.
          info
            Print information about Bach and the current project.
          tool
            Run provided tool with NAME passing any following arguments.
        """);
  }

  /** Default constructor usually used by the ServiceLoader facility. */
  public Main() {}

  @Override
  public String name() {
    return "bach";
  }

  public int run(String... args) {
    return run(Options.of(args));
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var options = Options.of(out, err, args);
    return run(options);
  }

  public int run(Options options) {
    if (options.flags().contains(Flag.VERBOSE)) options.out().println(options);
    if (options.args().isEmpty()) {
      options.out().println("No argument, no action.");
      return 0;
    }
    return run(Bach.of(options));
  }

  public int run(Bach bach) {
    var options = bach.options();
    if (options.printVersionAndExit() || options.printVersionAndContinue()) {
      options.out().print(Bach.version());
      if (options.printVersionAndExit()) return 0;
    }
    if (options.printHelpAndExit()) {
      help(options.out());
      return 0;
    }
    if (options.tool().isPresent()) {
      var command = options.tool().get();
      var recording = bach.run(command);
      if (!recording.errors().isEmpty()) bach.print(recording.errors());
      if (!recording.output().isEmpty()) bach.print(recording.output());
      if (recording.isError())
        bach.print("Tool %s returned exit code %d", command.name(), recording.code());
      return recording.code();
    }
    for (var action : options.actions()) {
      bach.debug("Perform main action: `%s`", action);
      try {
        switch (action) {
          case "build" -> bach.build();
          case "clean" -> bach.clean();
          case "info" -> bach.info();
          default -> {
            bach.print("Unsupported action: %s", action);
            return -1;
          }
        }
      } catch (Exception exception) {
        bach.print("Action %s failed: %s", action, exception);
        exception.printStackTrace(options.err());
        return 1;
      }
    }
    return 0;
  }
}
