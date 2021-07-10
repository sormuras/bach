package com.github.sormuras.bach.call;

import java.util.List;

public record ToolCall(String name, List<String> arguments) implements CallWith<ToolCall> {

  @Override
  public ToolCall arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new ToolCall(name, arguments);
  }
}
