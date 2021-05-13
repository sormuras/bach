package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.ToolCall;
import java.util.List;

public record JLinkCall(List<String> arguments) implements ToolCall<JLinkCall> {

  public JLinkCall() {
    this(List.of());
  }

  @Override
  public String name() {
    return "jlink";
  }

  @Override
  public JLinkCall arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new JLinkCall(arguments);
  }
}
