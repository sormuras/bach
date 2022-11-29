package run.bach;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.MODULE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Command.Container.class)
public @interface Command {
  /** The tool name of this tool call shortcut. */
  String name();

  /** A short description of the purpose of this tool call shortcut. */
  String description() default "";

  /**
   * The plus-separated tool calls as an array of strings.
   *
   * <p>Example:
   * <pre>{@code
   *    args = {"javac", "--version", "+", "jar", "--version"}
   * </pre>
   */
  String[] args() default {};

  /**
   * The line-separated tool calls with each line representing a space-separated tool call.
   *
   * <p>A tool call always starts with tool name and is followed by an arbitrary number of tool arguments.
   *
   * <p>Example:
   * <pre>{@code
   *    text = """
   *           javac  --version
   *           jar    --version
   *           """
   * </pre>
   */
  String line() default "";

  @Target(ElementType.MODULE)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Container {
    Command[] value();
  }
}
