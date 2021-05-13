package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.ToolCall;
import java.util.List;

public record JarCall(List<String> arguments) implements ToolCall<JarCall> {

  public JarCall() {
    this(List.of());
  }

  @Override
  public String name() {
    return "jar";
  }

  @Override
  public JarCall arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new JarCall(arguments);
  }
}
