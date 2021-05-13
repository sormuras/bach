package com.github.sormuras.bach.api;

import java.io.Serial;

public class UnsupportedWorkflowException extends BachException {
  @Serial
  private static final long serialVersionUID = 4804202645105941386L;

  public UnsupportedWorkflowException(String action) {
    super("Unsupported action: " + action);
  }
}