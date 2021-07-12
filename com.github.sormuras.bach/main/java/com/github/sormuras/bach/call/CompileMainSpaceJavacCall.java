package com.github.sormuras.bach.call;

import java.util.List;

public record CompileMainSpaceJavacCall(List<String> arguments)
    implements JavacCall<CompileMainSpaceJavacCall> {

  public CompileMainSpaceJavacCall() {
    this(List.of());
  }

  @Override
  public CompileMainSpaceJavacCall arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new CompileMainSpaceJavacCall(arguments);
  }
}
