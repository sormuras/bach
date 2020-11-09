package com.github.sormuras.bach.tool;

import java.time.Duration;
import java.util.List;

/**
 * Recordable tool call response.
 *
 * @param name the name of the called tool
 * @param args the arguments of the tool call run
 * @param thread the ID of the thread that ran the tool call
 * @param duration the duration of the tool call run
 * @param code the exit code of the tool call run
 * @param out the normal and expected output of the tool call run
 * @param err the error message of the tool call run
 */
public record ToolResponse(
    String name,
    List<String> args,
    long thread,
    Duration duration,
    int code,
    String out,
    String err) {

  /**
   * Returns {@code true} if this response represents an errored tool call run.
   *
   * @return {@code true} if the {@link #code} component holds a non-zero value, else {@code false}
   */
  public boolean isError() {
    return code != 0;
  }

  /**
   * Returns {@code true} if this response represents a successful tool call run.
   *
   * @return {@code true} if the {@link #code} component is zero, else {@code false}
   */
  public boolean isSuccessful() {
    return code == 0;
  }

  /**
   * Returns silently if this response represents a successful tool call run.
   *
   * @throws RuntimeException if {@link #isError()} returns {@code true}
   */
  public void checkSuccessful() {
    if (isSuccessful()) return;
    throw new RuntimeException(name + " returned error code " + code + "\n" + toString().indent(4));
  }
}
