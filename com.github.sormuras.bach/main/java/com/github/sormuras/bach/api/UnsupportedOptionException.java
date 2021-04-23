package com.github.sormuras.bach.api;

import java.io.Serial;

public class UnsupportedOptionException extends BachException {
  @Serial
  private static final long serialVersionUID = 2673989659261329413L;

  public UnsupportedOptionException(String option) {
    super("Unsupported option: " + option);
  }
}