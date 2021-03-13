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
      var repeatable = property.repeatable ? " (repeatable option)" : "";
      properties.add(Options.key(property) + " VALUE" + repeatable);
      properties.add("  " + property.help);
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
          format
            Format source code files.
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
    var out = options.out();
    out.printf("Bach %s (%s)%n", Bach.version(), Bach.bin().toUri());
    if (options.is(Flag.VERBOSE)) {
      out.printf("  version = %s%n", Bach.version());
      out.printf("  bin = %s%n", Bach.bin());
      out.printf("  info = %s%n", options.info());
      out.printf("  flags = %s%n", options.flags());
      out.printf("  properties = %s%n", options.properties());
      out.printf("  actions = %s%n", options.actions());
      out.printf("  tool = %s%n", options.tool());
    }
    return run(Bach.of(options));
  }

  public int run(Bach bach) {
    try (bach) {
      return runAndReturnStatus(bach).ordinal();
    }
  }

  private Status runAndReturnStatus(Bach bach) {
    if (bach.is(Flag.VERSION) || bach.is(Flag.SHOW_VERSION)) {
      bach.say("%s", Bach.version());
      if (bach.is(Flag.VERSION /* and exit */)) return Status.OK;
    }
    if (bach.is(Flag.HELP /* and exit */)) {
      bach.say("%s", computeHelpMessage());
      return Status.OK;
    }
    if (bach.options().tool().isPresent()) {
      var command = bach.options().tool().get();
      var recording = bach.run(command);
      if (!recording.errors().isEmpty()) bach.say(recording.errors());
      if (!recording.output().isEmpty()) bach.say(recording.output());
      if (recording.isSuccessful()) return Status.OK;
      bach.say("Tool %s returned exit code %d", command.name(), recording.code());
      return Status.TOOL_FAILED;
    }
    for (var action : bach.options().actions()) {
      bach.log("Perform main action: `%s`", action);
      try {
        switch (action) {
          case "build" -> bach.build();
          case "clean" -> bach.clean();
          case "format" -> bach.format();
          case "info" -> bach.info();
          default -> {
            bach.say("Unsupported action: %s", action);
            return Status.ACTION_UNSUPPORTED;
          }
        }
      } catch (Exception exception) {
        bach.say("Action %s failed: %s", action, exception);
        exception.printStackTrace(bach.options().err());
        return Status.ACTION_FAILED;
      }
    }
    return Status.OK;
  }

  private enum Status {
    OK,
    ACTION_FAILED,
    ACTION_UNSUPPORTED,
    TOOL_FAILED
  }
}
