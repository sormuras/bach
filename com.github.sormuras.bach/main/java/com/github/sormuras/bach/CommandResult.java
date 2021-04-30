package com.github.sormuras.bach;

import java.time.Duration;
import java.util.List;

/**
 * A recording of a command run result.
 *
 * @param name the name of the tool
 * @param args the arguments of the tool run
 * @param thread the ID of the thread that ran the tool
 * @param duration the duration of the tool run
 * @param code the exit code of the tool run
 * @param output the normal and expected output of the tool run
 * @param errors the error message of the tool run
 */
public record CommandResult(
    String name,
    List<String> args,
    long thread,
    Duration duration,
    int code,
    String output,
    String errors) {

  /** {@return {@code true} if this recording represents an errored tool run} */
  public boolean isError() {
    return code != 0;
  }

  /** {@return {@code true} if this recording represents a successful tool run} */
  public boolean isSuccessful() {
    return code == 0;
  }

  /**
   * Returns silently if this recording represents a successful tool run.
   *
   * @throws RuntimeException if {@link #isError()} returns {@code true}
   */
  public void requireSuccessful() {
    if (isSuccessful()) return;
    throw new RuntimeException(name + " returned code " + code + "\n" + toString().indent(4));
  }
}
