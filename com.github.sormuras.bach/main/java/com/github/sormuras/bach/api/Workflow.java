package com.github.sormuras.bach.api;

import java.util.Locale;

public enum Workflow {
  BUILD,
  CLEAN,
  COMPILE_MAIN,
  COMPILE_TEST,
  EXECUTE_TESTS,
  GENERATE_DOCUMENTATION,
  GENERATE_IMAGE,
  WRITE_LOGBOOK;

  public static Workflow ofCli(String string) {
    var name = string.toUpperCase(Locale.ROOT).replace('-', '_');
    try {
      return Workflow.valueOf(name);
    } catch (IllegalArgumentException exception) {
      throw new UnsupportedWorkflowException(string);
    }
  }

  public static String toCli(Workflow workflow) {
    return workflow.name().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  public String cli() {
    return toCli(this);
  }
}
