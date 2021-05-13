package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.ToolCall;
import java.util.List;

public record JavacCall(List<String> arguments) implements ToolCall<JavacCall> {

  public JavacCall() {
    this(List.of());
  }

  @Override
  public String name() {
    return "javac";
  }

  @Override
  public JavacCall arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new JavacCall(arguments);
  }
}
