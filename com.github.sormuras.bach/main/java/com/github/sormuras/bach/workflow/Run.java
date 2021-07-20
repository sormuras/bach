package com.github.sormuras.bach.workflow;

import java.time.Duration;
import java.util.List;

/**
 * A recording of a tool call run.
 *
 * @param name     the name of the tool
 * @param args     the arguments of the tool run
 * @param thread   the ID of the thread that ran the tool
 * @param duration the duration of the tool run
 * @param code     the exit code of the tool run
 * @param output   the normal and expected output of the tool run
 * @param errors   the error message of the tool run
 */
public record Run(
    String name,
    List<String> args,
    long thread,
    Duration duration,
    int code,
    String output,
    String errors) {

  /**
   * {@return {@code true} if this response represents an errored tool call run}
   */
  public boolean isError() {
    return code != 0;
  }

  /**
   * {@return {@code true} if this response represents a successful tool call run}
   */
  public boolean isSuccessful() {
    return code == 0;
  }

  /**
   * Returns silently if this response represents a successful tool call run.
   *
   * @throws RuntimeException if {@link #isError()} returns {@code true}
   */
  public void requireSuccessful() {
    if (isSuccessful()) return;
    var message = "%s returned code %d\n%s".formatted(name, code, toString().indent(4));
    throw new RuntimeException(message);
  }
}
