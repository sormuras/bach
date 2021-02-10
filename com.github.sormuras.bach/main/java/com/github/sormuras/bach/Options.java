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

/**
 * @param out a stream to which "expected" output should be written
 * @param err a stream to which any error messages should be written
 * @param configuration project-info module's name passed via {@code --configuration MODULE}
 * @param flags a set of feature toggles passed individually like {@code --verbose --help ...}
 * @param projectName project's name passed via {@code --project-name NAME}
 * @param projectVersion project's version passed via {@code --project-version VERSION}
 * @param actions the list of actions to execute passed individually like {@code info build ...}
 * @param tool an optional command to execute passed via {@code tool NAME [ARGS...]}
 */
public record Options(
    PrintWriter out,
    PrintWriter err,
    String configuration,
    Set<Flag> flags,
    String projectName,
    String projectVersion,
    List<String> actions,
    Optional<Tool> tool) {

  /**
   * {@return {@code true} if the given flag is present within the set of flags, else {@code false}}
   *
   * @param flag a binary option to check for being present
   */
  public boolean is(Flag flag) {
    return flags.contains(flag);
  }

  /**
   * {@return an instance of {@code Options} parsed from the given command-line arguments}
   *
   * <p>This convenient overload wraps the {@link System#out} and {@link System#err} streams into
   * automatically flushing {@link PrintWriter} objects and delegates to {@link #of(PrintWriter,
   * PrintWriter, String...)}.
   *
   * @param args the command-line arguments to parse
   */
  public static Options of(String... args) {
    return of(new PrintWriter(System.out, true), new PrintWriter(System.err, true), args);
  }

  /**
   * {@return an instance of {@code Options} parsed from the given command-line arguments}
   *
   * @param out a stream to which "expected" output should be written
   * @param err a stream to which any error messages should be written
   * @param args the command-line arguments to parse
   */
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
        configuration.get(),
        flags,
        projectName.get(),
        projectVersion.get(),
        actions,
        Optional.ofNullable(tool.get()));
  }

  private static String next(ArrayDeque<String> deque, String message) {
    if (deque.isEmpty()) throw new IllegalArgumentException(message);
    return deque.removeFirst();
  }

  /** Feature toggle. */
  public enum Flag {

    /**
     * Print messages about what Bach is doing.
     *
     * @see Bach#debug(String, Object...)
     */
    VERBOSE("Print messages about what Bach is doing."),

    /** Mute all normal (expected) printouts. */
    SILENT("Mute all normal (expected) printouts."),

    /** Print Bach's version and exit. */
    VERSION("Print Bach's version and exit."),

    /** Print Bach's version and continue. */
    SHOW_VERSION("Print Bach's version and continue."),

    /** Print usage information and exit. */
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
