package com.github.sormuras.bach;

import java.time.Duration;
import java.util.Arrays;
import java.util.StringJoiner;

/** Recordable tool call response. */
public final class ToolResponse {
  private final String name;
  private final String[] args;
  private final long thread;
  private final Duration duration;
  private final int code;
  private final String out;
  private final String err;

  /**
   * Canoncial tool response constructor.
   *
   * @param name Name of the called tool
   * @param args Arguments of the tool call run
   * @param thread Thread that ran the tool call
   * @param duration Duration of the tool call run
   * @param code Exit code of the tool call run
   * @param out Normal and expected output of the tool call run
   * @param err Error messages of the tool call run
   */
  public ToolResponse(
      String name,
      String[] args,
      long thread,
      Duration duration,
      int code,
      String out,
      String err) {
    this.name = name;
    this.args = args;
    this.thread = thread;
    this.duration = duration;
    this.code = code;
    this.out = out;
    this.err = err;
  }

  /**
   * Return the normal and expected output of the tool call run.
   *
   * @return a non-empty string representing the normal and expected output of the tool call run
   */
  public String out() {
    return out;
  }

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
   * @throws RuntimeException If {@link #isError()} returns {@code true}
   */
  public void checkSuccessful() {
    if (isSuccessful()) return;
    throw new RuntimeException(name + " returned error code " + code);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ToolResponse.class.getSimpleName() + "[", "]")
        .add("name='" + name + "'")
        .add("args=" + Arrays.toString(args))
        .add("thread=" + thread)
        .add("duration=" + duration)
        .add("code=" + code)
        .add("out='" + out + "'")
        .add("err='" + err + "'")
        .toString();
  }
}
