package com.github.sormuras.bach;

import com.github.sormuras.bach.tool.Tool;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public record Options(
    // a stream to which "expected" output should be written
    PrintWriter out,
    // a stream to which any error messages should be written
    PrintWriter err,
    // all arguments (raw)
    List<String> args,
    // "--configuration" "configuration"
    String configuration,
    // flags, like "--verbose", "--run-commands-sequentially"
    Set<Flag> flags,
    // "--project-name"
    String projectName,
    // "--project-version"
    String projectVersion,
    // "tool NAME [ARGS...]"
    Optional<Tool> tool,
    // actions
    List<String> actions) {

  public boolean is(Flag flag) {
    return flags.contains(flag);
  }

  public static Options of(String... args) {
    return of(new PrintWriter(System.out, true), new PrintWriter(System.err, true), args);
  }

  public static Options of(PrintWriter out, PrintWriter err, String... args) {
    var deque = new ArrayDeque<>(List.of(args));
    var configuration = new AtomicReference<>("configuration");
    var projectName = new AtomicReference<>("noname");
    var projectVersion = new AtomicReference<>("0");
    var tool = new AtomicReference<Tool>(null);
    var flags = new HashSet<Flag>();
    var actions = new ArrayList<String>();
    while (!deque.isEmpty()) {
      var argument = deque.removeFirst();
      switch (argument) {
        case "--configuration" -> configuration.set(next(deque, "--configuration MODULE"));
        case "--project-name" -> projectName.set(next(deque, "--project-name NAME"));
        case "--project-version" -> projectVersion.set(next(deque, "--project-version VERSION"));
        case "tool" -> {
          var name = next(deque, "No tool name given: bach <OPTIONS...> tool NAME <ARGS...>");
          tool.set(Command.of(name, deque.toArray(String[]::new)));
          deque.clear();
        }
        default -> {
          if (argument.startsWith("--")) { // handle flags
            var name = argument.substring(2).toUpperCase(Locale.ROOT).replace('-', '_');
            var flag = Flag.valueOf(name); // throws IllegalArgumentException
            flags.add(flag);
            continue;
          }
          actions.add(argument);
          actions.addAll(deque);
          deque.clear();
        }
      }
    }
    return new Options(
        flags.contains(Flag.SILENT) ? new PrintWriter(Writer.nullWriter()) : out,
        err,
        List.of(args),
        configuration.get(),
        flags,
        projectName.get(),
        projectVersion.get(),
        Optional.ofNullable(tool.get()),
        actions);
  }

  private static String next(ArrayDeque<String> deque, String message) {
    if (deque.isEmpty()) throw new IllegalArgumentException(message);
    return deque.removeFirst();
  }

  /**
   * Feature toggle.
   */
  public enum Flag {

    /**
     * Print messages about what Bach is doing.
     *
     * @see Bach#debug(String, Object...)
     */
    VERBOSE("Print messages about what Bach is doing."),

    /**
     * Mute all normal (expected) printouts.
     */
    SILENT("Mute all normal (expected) printouts."),

    /**
     * Print Bach's version and exit.
     */
    VERSION("Print Bach's version and exit."),

    /**
     * Print Bach's version and continue.
     */
    SHOW_VERSION("Print Bach's version and continue."),

    /**
     * Print usage information and exit.
     */
    HELP("Print usage information and exit."),

    /**
     * Prevent parallel execution of commands.
     *
     * @see Bach#run(Command, Command[])
     * @see Bach#run(List)
     */
    RUN_COMMANDS_SEQUENTIALLY("Prevent parallel execution of commands.");

    final String help;

    Flag(String help) {
      this.help = help;
    }

    String help() {
      return help;
    }
  }
}
