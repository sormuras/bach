package com.github.sormuras.bach.call;

import java.util.List;

public record JavacCall(List<String> arguments) implements AnyCall<JavacCall> {

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
