package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.ToolCall;
import java.util.List;

public record JDepsCall(List<String> arguments) implements ToolCall<JDepsCall> {

  public JDepsCall() {
    this(List.of());
  }

  @Override
  public String name() {
    return "jdeps";
  }

  @Override
  public JDepsCall arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new JDepsCall(arguments);
  }
}
