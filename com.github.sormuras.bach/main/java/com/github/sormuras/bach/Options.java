package com.github.sormuras.bach;

import com.github.sormuras.bach.tool.Tool;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * An options descriptor.
 *
 * @param info an optional project declaration instance, may be {@code null}
 * @param out a stream to which "expected" output should be written
 * @param err a stream to which any error messages should be written
 * @param flags a set of feature toggles passed individually like {@code --verbose --help ...}
 * @param actions the list of actions to execute passed individually like {@code info build ...}
 * @param tool an optional command to execute passed via {@code tool NAME [ARGS...]}
 */
public record Options(
    Optional<ProjectInfo> info,
    PrintWriter out,
    PrintWriter err,
    Set<Flag> flags,
    Map<Property, List<String>> properties,
    List<String> actions,
    Optional<Tool> tool) {

  public Options with(ProjectInfo info) {
    return new Options(Optional.ofNullable(info), out, err, flags, properties, actions, tool);
  }

  /**
   * {@return new {@code Options} instance with the given flags added}
   *
   * @param flag a flag constant to add to the set of flags of this options object
   * @param more more flags to add
   */
  public Options with(Flag flag, Flag... more) {
    var flags = new HashSet<>(this.flags);
    flags.add(flag);
    flags.addAll(Set.of(more));
    return new Options(info, out, err, flags, properties, actions, tool);
  }

  public List<String> values(Property property) {
    return properties.getOrDefault(property, List.of());
  }

  public String get(Property property, String defaultValue) {
    var values = values(property);
    return values.isEmpty() ? defaultValue : values.get(0);
  }

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
    var mapOfFlagArguments = map(Flag.class);
    var flags = new TreeSet<Flag>();
    var mapOfPropertyArguments = map(Property.class);
    var properties = new TreeMap<Property, List<String>>();
    var actions = new ArrayList<String>();
    var tool = new AtomicReference<Tool>(null);

    while (!deque.isEmpty()) {
      var argument = deque.removeFirst();
      if (argument.isBlank()) throw new IllegalArgumentException("An argument must not be blank");

      if (argument.startsWith("--")) { // handle flags and properties
        var flag = mapOfFlagArguments.get(argument);
        if (flag != null) {
          flags.add(flag);
          continue;
        }
        var property = mapOfPropertyArguments.get(argument);
        if (property != null) {
          var value = next(deque, property.name());
          properties.merge(property, List.of(value), Options::merge);
          continue;
        }
        throw new IllegalArgumentException("No flag, no property: " + argument);
      }

      if ("tool".equals(argument)) {
        var name = next(deque, "No tool name given: bach <OPTIONS...> tool NAME <ARGS...>");
        tool.set(Command.of(name, deque.toArray(String[]::new)));
        deque.clear();
        continue;
      }

      actions.add(argument);
      actions.addAll(deque);
      deque.clear();
    }

    return new Options(
        Optional.empty(),
        flags.contains(Flag.SILENT) ? new PrintWriter(Writer.nullWriter()) : out,
        err,
        flags,
        properties,
        actions,
        Optional.ofNullable(tool.get()));
  }

  private static String next(ArrayDeque<String> deque, String message) {
    if (deque.isEmpty()) throw new IllegalArgumentException(message);
    return deque.removeFirst();
  }

  public static <E extends Enum<E>> String key(E constant) {
    return "--" + constant.name().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  private static <E extends Enum<E>> TreeMap<String, E> map(Class<? extends E> enums) {
    var map = new TreeMap<String, E>();
    for (var constant : enums.getEnumConstants()) map.put(key(constant), constant);
    return map;
  }

  private static <E> List<E> merge(List<E> head, List<E> tail) {
    return Stream.concat(head.stream(), tail.stream()).toList();
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
     * Activate all verification measures available.
     *
     * <p>When Bach runs in strict mode the following default implementations change their behaviour
     * into a more defensive way of doing things:
     *
     * <ul>
     *   <li>Source code style is checked via running a formatter in `VERIFY` mode.
     *   <li>Only explicitly declared requires are considered when downloading external modules.
     *   <li>All external modular JAR files are verified to conform with expected metadata.
     * </ul>
     *
     * @see ProjectInfo#format()
     * @see ProjectInfo#metadata()
     */
    STRICT("Activate all verification measures available."),

    /**
     * Prevent parallel execution of commands.
     *
     * @see Bach#run(Command, Command[])
     * @see Bach#run(Stream)
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

  /** Key-value pair option. */
  public enum Property {
    /** Name of the module to load on startup passed via {@code --bach-info MODULE}. */
    BACH_INFO("Specify the module to load on startup, defaults to \"" + ProjectInfo.MODULE + "\""),

    /** Root directory of the project, defaults to user's current directory. */
    PROJECT_ROOT("Specify the root directory of the project, defaults to user's current directory"),

    /**
     * Project's name passed via {@code --project-name NAME}.
     *
     * @see ProjectInfo#name()
     */
    PROJECT_NAME("Specify the name of the project."),

    /**
     * Project's version passed via {@code --project-version VERSION}.
     *
     * @see ProjectInfo#version()
     */
    PROJECT_VERSION("Specify the version of the project."),

    /**
     * Compile main modules for specified Java release.
     *
     * @see ProjectInfo#compileModulesForJavaRelease()
     */
    PROJECT_TARGETS_JAVA("Compile main modules for specified Java release."),

    /** Skip all executions for the specified tool, this option is repeatable */
    SKIP_TOOL("Skip all executions of the specified tool.", true);

    final String help;
    final boolean repeatable;

    Property(String help) {
      this(help, false);
    }

    Property(String help, boolean repeatable) {
      this.help = help;
      this.repeatable = repeatable;
    }

    String help() {
      return help;
    }

    boolean repeatable() {
      return repeatable;
    }
  }
}
