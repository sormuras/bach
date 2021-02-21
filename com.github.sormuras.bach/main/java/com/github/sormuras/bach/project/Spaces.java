package com.github.sormuras.bach.project;

public record Spaces(JavaStyle style) {
  public static Spaces of() {
    return new Spaces(JavaStyle.FREE);
  }
}
