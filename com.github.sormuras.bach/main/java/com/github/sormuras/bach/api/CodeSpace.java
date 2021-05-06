package com.github.sormuras.bach.api;

import java.util.Locale;

public enum CodeSpace {
  MAIN(""),
  TEST("-test");

  public static CodeSpace ofCli(String cli) {
    return valueOf(cli.toUpperCase(Locale.ROOT));
  }

  private final String suffix;

  CodeSpace(String suffix) {
    this.suffix = suffix;
  }

  public String suffix() {
    return suffix;
  }
}
