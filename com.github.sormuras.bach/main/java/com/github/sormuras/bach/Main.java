package com.github.sormuras.bach;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

/** Bach's main program. */
public record Main() implements ToolProvider {

  /** The main entry-point. */
  public static void main(String... args) {
    var options = args.length == 0 ? Options.parse("--help") : Options.parse(args);
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    try (var program = new Program(out, err, options)) {
      program.run();
    }
  }

  @Override
  public String name() {
    return "bach";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    try (var program = new Program(out, err, Options.parse(args))) {
      program.run();
      return 0;
    } catch (Exception exception) {
      err.println(exception);
      return 1;
    }
  }

  record Program(PrintWriter out, PrintWriter err, Options options) implements AutoCloseable {

    public void run() {
      var main = options.forMain();
      if (main.help().orElse(false)) {
        out.print(
            """
            Usage: bach OPTION [ARGUMENTS...]
            Options include:
              --help, /?
                Print this help message.
              --list-tools
                Print provided tools and exit.
              --run-tool NAME ARGUMENTS...
                Find first tool provider by its NAME and run it with the given ARGUMENTS.
              --show-tools
                Print provided tools and continue.
              --version
                Print Bach's version and exit.
            """);
        return;
      }
      Bach bach = null; // create lazily and memoize locally
      if (main.listTools().orElse(false) || main.showTools().orElse(false)) {
        bach = new Bach();
        bach.printer().printTools();
        if (main.listTools().orElse(false)) return;
      }
      if (main.version().orElse(false)) {
        out.println(Bach.version());
        return;
      }
      var unhandled = options.unhandledArguments();
      if (main.runTool().isPresent()) {
        if (bach == null) bach = new Bach();
        var tool = main.runTool().get();
        var command = Command.of(tool).addAll(unhandled);
        var run = bach.run(command);
        bach.printer().print(run, true, 0);
        return;
      }
      if (unhandled.isEmpty()) {
        throw new AssertionError("Something bad went wrong: %s".formatted(options));
      }
      throw new UnsupportedOperationException(String.join(" ", unhandled));
    }

    @Override
    public void close() {
      out.flush();
      err.flush();
    }
  }
}
