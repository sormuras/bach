/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

/** Unchecked exception thrown when a tool could not be found. */
public class ToolNotFoundException extends RuntimeException {

  @java.io.Serial private static final long serialVersionUID = -8121824957275347277L;

  public ToolNotFoundException(String message) {
    super(message);
  }
}
