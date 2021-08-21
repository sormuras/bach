package com.github.sormuras.bach;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * A record of a tool run.
 *
 * @param name the name of the tool
 * @param args the arguments of the tool run
 * @param thread the ID of the thread that ran the tool
 * @param duration the duration of the tool run
 * @param code the exit code of the tool run
 * @param output the normal and expected output of the tool run
 * @param errors the error message of the tool run
 */
public record ToolRun(
    String name,
    List<String> args,
    long thread,
    Duration duration,
    int code,
    String output,
    String errors) {

  /** {@return {@code true} if this response represents an errored tool call run} */
  public boolean isError() {
    return code != 0;
  }

  /** {@return {@code true} if this response represents a successful tool call run} */
  public boolean isSuccessful() {
    return code == 0;
  }

  /**
   * {@return silently this instance if it represents a successful tool call run}
   *
   * @throws RuntimeException if {@link #isError()} returns {@code true}
   */
  public ToolRun requireSuccessful() {
    if (isSuccessful()) return this;
    var message = "%s returned code %d\n%s".formatted(name, code, toString().indent(4));
    throw new RuntimeException(message);
  }

  public ToolRun visit(Consumer<ToolRun> visitor) {
    visitor.accept(this);
    return this;
  }
}
