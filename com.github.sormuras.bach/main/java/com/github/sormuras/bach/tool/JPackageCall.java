package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.ToolCall;
import java.util.List;

public record JPackageCall(List<String> arguments) implements ToolCall<JPackageCall> {

  public JPackageCall() {
    this(List.of());
  }

  @Override
  public String name() {
    return "jpackage";
  }

  @Override
  public JPackageCall arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new JPackageCall(arguments);
  }
}
