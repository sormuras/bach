package run.bach;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import run.duke.ToolCall;
import run.duke.ToolCalls;

@Target(ElementType.MODULE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Command.Container.class)
public @interface Command {
  /** The nickname of this tool call shortcut. */
  String name();

  /** A short description of the purpose of this tool call shortcut. */
  String description() default "";

  /**
   * The plus-separated tool calls as an array of strings.
   *
   * <p>Example:
   * <pre>{@code
   *    args = "javac --version + jar --version"
   *    args = {"javac --version", "jar --version"}
   *    args = {"javac", "--version", "+", "jar", "--version"}
   * </pre>
   */
  String[] args();

  /** Args mode. */
  Mode mode() default Mode.AUTO;

  enum Mode implements Function<String[], ToolCalls> {
    /** Default mode. */
    AUTO {
      @Override
      public ToolCalls apply(String[] args) {
        if (args.length == 0) return ToolCalls.of();
        return Mode.detect(args).orElseThrow().apply(args);
      }
    },

    /**
     * One string per argument.
     *
     * <p>Example: {@code {"javac", "--version", "+", "jar", "--version"}}
     */
    MAIN {
      @Override
      public ToolCalls apply(String[] args) {
        return ToolCalls.of(args);
      }
    },

    /**
     * One tool call per argument.
     *
     * <p>Example: {@code {"javac --version", "jar --version"}}
     */
    LIST {
      @Override
      public ToolCalls apply(String[] args) {
        return new ToolCalls(Stream.of(args).map(ToolCall::ofCommandLine).toList());
      }
    },

    /**
     * One string per command-line.
     *
     * <p>Example: {@code "javac --version + jar --version"}
     */
    LINE {
      @Override
      public ToolCalls apply(String[] args) {
        var split = String.join(" + ", args).split("\\s+");
        return ToolCalls.of(split);
      }
    };

    public static Optional<Mode> detect(String... args) {
      if (args.length == 0) return Optional.empty();
      var first = args[0];
      if (args.length == 1) {
        return Optional.of(first.contains(" + ") ? LINE : first.contains(" ") ? LIST : MAIN);
      }
      for (var arg : args) {
        if (arg.equals("+")) return Optional.of(MAIN);
        if (arg.contains(" ")) return Optional.of(LIST);
      }
      return Optional.of(MAIN);
    }
  }

  @Target(ElementType.MODULE)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Container {
    Command[] value();
  }
}
