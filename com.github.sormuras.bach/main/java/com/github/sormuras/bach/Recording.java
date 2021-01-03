package com.github.sormuras.bach;

import java.time.Duration;

/**
 * A recording of a tool run.
 *
 * @param name the name of the tool
 * @param args the arguments of the tool run
 * @param thread the ID of the thread that ran the tool
 * @param duration the duration of the tool run
 * @param code the exit code of the tool run
 * @param output the normal and expected output of the tool run
 * @param errors the error message of the tool run
 */
public record Recording(
    String name,
    String[] args,
    long thread,
    Duration duration,
    int code,
    String output,
    String errors) {

  /**
   * Returns {@code true} if this recording represents an errored tool run.
   *
   * @return {@code true} if the {@link #code} component holds a non-zero value, else {@code false}
   */
  public boolean isError() {
    return code != 0;
  }

  /**
   * Returns {@code true} if this recording represents a successful tool run.
   *
   * @return {@code true} if the {@link #code} component is zero, else {@code false}
   */
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
    throw new RuntimeException(name + " returned error code " + code + "\n" + toString().indent(4));
  }
}
