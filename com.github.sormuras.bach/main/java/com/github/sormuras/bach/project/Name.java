package com.github.sormuras.bach.project;

public record Name(String name) {
  public static Name of(String name) {
    return new Name(name);
  }
}
