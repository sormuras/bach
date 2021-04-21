package com.github.sormuras.bach.api;

public enum CodeSpace {
  MAIN(""),
  TEST("-test");

  private final String suffix;

  CodeSpace(String suffix) {
    this.suffix = suffix;
  }

  public String suffix() {
    return suffix;
  }
}
