package com.github.sormuras.bach.project;

public record Name(String value) {
  public static Name of(String name) {
    return new Name(name);
  }
}
