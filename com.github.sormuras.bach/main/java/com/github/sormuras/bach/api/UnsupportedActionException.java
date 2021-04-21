package com.github.sormuras.bach.api;

import java.io.Serial;

public class UnsupportedActionException extends BachException {
  @Serial
  private static final long serialVersionUID = 4804202645105941386L;

  public UnsupportedActionException(String action) {
    super("Unsupported action: " + action);
  }
}