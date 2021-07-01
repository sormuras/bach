package com.github.sormuras.bach.call;

import java.util.List;

public record JarCall(List<String> arguments) implements AnyCall<JarCall> {

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
