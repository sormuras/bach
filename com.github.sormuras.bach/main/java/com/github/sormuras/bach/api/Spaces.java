package com.github.sormuras.bach.api;

public record Spaces(CodeSpaceMain main, CodeSpaceTest test) {
  public static Spaces of(CodeSpaceMain main, CodeSpaceTest test) {
    return new Spaces(main, test);
  }
}
