package com.github.sormuras.bach.project;

/** Test code space. */
public record TestSpace(Tweaks tweaks) implements Space {
  @Override
  public String name() {
    return "test";
  }
}
