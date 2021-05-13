package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.ToolCall;
import java.util.List;

/** A nonimal tool call with arbitrary arguments. */
public record AnyCall(String name, List<String> arguments) implements ToolCall<AnyCall> {

  public AnyCall(String name) {
    this(name, List.of());
  }

  @Override
  public AnyCall arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new AnyCall(name, arguments);
  }
}
