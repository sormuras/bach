package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.ToolCall;
import java.util.List;

public record JavaCall(List<String> arguments) implements ToolCall<JavaCall> {

  public JavaCall() {
    this(List.of());
  }

  @Override
  public String name() {
    return "java";
  }

  @Override
  public JavaCall arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new JavaCall(arguments);
  }
}
