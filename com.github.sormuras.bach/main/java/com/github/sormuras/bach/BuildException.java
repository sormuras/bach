package com.github.sormuras.bach;

import java.io.Serial;

/** An exception thrown at build time. */
public class BuildException extends RuntimeException {

  @Serial private static final long serialVersionUID = 3694707496152575831L;

  /**
   * Initialize this build-related exception with the specified detail message.
   *
   * @param message the detail message
   */
  public BuildException(String message) {
    super(message);
  }
}
