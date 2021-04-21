package com.github.sormuras.bach.api;

import java.io.Serial;

public class BachException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 210026486670031111L;

  public BachException(String message) {
    super(message);
  }

  public BachException(String message, Throwable cause) {
    super(message, cause);
  }
}
