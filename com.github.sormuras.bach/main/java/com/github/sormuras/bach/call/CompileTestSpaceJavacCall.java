package com.github.sormuras.bach.call;

import java.util.List;

public record CompileTestSpaceJavacCall(List<String> arguments)
    implements JavacCall<CompileTestSpaceJavacCall> {

  public CompileTestSpaceJavacCall() {
    this(List.of());
  }

  @Override
  public CompileTestSpaceJavacCall arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new CompileTestSpaceJavacCall(arguments);
  }
}
