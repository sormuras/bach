package com.github.sormuras.bach.call;

import java.util.List;

public record DefaultJavacCall(List<String> arguments) implements JavacCall<DefaultJavacCall> {

  public DefaultJavacCall() {
    this(List.of());
  }

  @Override
  public DefaultJavacCall arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new DefaultJavacCall(arguments);
  }
}
