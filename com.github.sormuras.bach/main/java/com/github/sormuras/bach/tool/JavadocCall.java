package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.ToolCall;
import java.util.List;

public record JavadocCall(List<String> arguments) implements ToolCall<JavadocCall> {

  public JavadocCall() {
    this(List.of());
  }

  @Override
  public String name() {
    return "javadoc";
  }

  @Override
  public JavadocCall arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new JavadocCall(arguments);
  }
}
