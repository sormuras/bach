package com.github.sormuras.bach;

import com.github.sormuras.bach.Options.Flag;
import com.github.sormuras.bach.Options.Property;
import java.io.PrintWriter;
import java.util.StringJoiner;
import java.util.spi.ToolProvider;

/** Bach's main program. */
public class Main implements ToolProvider {

  public static void main(String... args) {
    System.exit(new Main().run(args));
  }

  public static String computeHelpMessage() {
    var flags = new StringJoiner("\n");
    for (var flag : Flag.values()) flags.add(Options.key(flag)).add("  " + flag.help());

    var properties = new StringJoiner("\n");
    for (var property : Property.values()) {
      var repeatable = property.repeatable() ? " (repeatable option)" : "";
      properties.add(Options.key(property) + " VALUE" + repeatable);
      properties.add("  " + property.help());
    }

    return """
        Usage: bach [OPTIONS] ACTION [ACTIONS...]
                 to execute one or more actions in sequence
           or: bach [OPTIONS] [ACTIONS...] tool NAME [ARGS...]
                 to execute a provided tool with tool-specific arguments

        Options include the following flags:

        {{FLAGS}}

        Options include the following key-value pairs:

        {{PROPERTIES}}

        Actions include:

          build
            Build the current project.
          clean
            Delete workspace directory.
          info
            Print information about Bach and the current project.
          tool
            Run provided tool with NAME passing any following arguments.
        """
        .replace("{{FLAGS}}", flags.toString().indent(2).stripTrailing())
        .replace("{{PROPERTIES}}", properties.toString().indent(2).stripTrailing());
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
    return run(Options.of(out, err, args));
  }

  public int run(Options options) {
    if (options.is(Flag.VERBOSE)) options.out().println(options);
    return run(Bach.of(options));
  }

  public int run(Bach bach) {
    if (bach.is(Flag.VERSION) || bach.is(Flag.SHOW_VERSION)) {
      bach.print("%s", Bach.version());
      if (bach.is(Flag.VERSION /* and exit */)) return 0;
    }
    if (bach.is(Flag.HELP /* and exit */)) {
      bach.print("%s", computeHelpMessage());
      return 0;
    }
    if (bach.options().tool().isPresent()) {
      var command = bach.options().tool().get();
      var recording = bach.run(command);
      if (!recording.errors().isEmpty()) bach.print(recording.errors());
      if (!recording.output().isEmpty()) bach.print(recording.output());
      if (recording.isError())
        bach.print("Tool %s returned exit code %d", command.name(), recording.code());
      return recording.code();
    }
    for (var action : bach.options().actions()) {
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
        exception.printStackTrace(bach.options().err());
        return 1;
      }
    }
    return 0;
  }
}
