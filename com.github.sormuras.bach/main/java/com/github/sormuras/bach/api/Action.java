package com.github.sormuras.bach.api;

import java.util.Locale;

public enum Action {
  BUILD,
  CLEAN,
  COMPILE_MAIN,
  COMPILE_TEST,
  EXECUTE_TESTS,
  GENERATE_DOCUMENTATION,
  GENERATE_IMAGE,
  WRITE_LOGBOOK;

  public static Action ofCli(String string) {
    var name = string.toUpperCase(Locale.ROOT).replace('-', '_');
    try {
      return Action.valueOf(name);
    } catch (IllegalArgumentException exception) {
      throw new UnsupportedActionException(string);
    }
  }

  public static String toCli(Action action) {
    return action.name().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  public String cli() {
    return toCli(this);
  }
}
