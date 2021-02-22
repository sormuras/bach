package com.github.sormuras.bach.project;

/** A record of code spaces and code related properties. */
public record Spaces(JavaStyle style) {
  public static Spaces of() {
    return new Spaces(JavaStyle.FREE);
  }
}
