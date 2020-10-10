package com.github.sormuras.bach;

import java.time.Duration;
import java.util.Arrays;
import java.util.StringJoiner;

public final class ToolResponse {
  private final String name;
  private final String[] args;
  private final long thread;
  private final Duration duration;
  private final int code;
  private final String out;
  private final String err;

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

  public String out() {
    return out;
  }

  public boolean isError() {
    return code != 0;
  }

  public boolean isSuccessful() {
    return code == 0;
  }

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
