/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.lang.System.Logger.Level;

public interface Action {
  Workflow workflow();

  default void log(String message) {
    log(Level.DEBUG, message);
  }

  default void log(Level level, String message) {
    var severity = level.getSeverity();
    if (severity <= Level.DEBUG.getSeverity()) return;
    var stream = severity < Level.ERROR.getSeverity() ? System.out : System.err;
    stream.println(message);
  }
}
