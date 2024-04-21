/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.lang.System.Logger.Level;
import run.bach.ToolCall;

public interface Action {
  Workflow workflow();

  default void say(String message) {
    workflow().runner().log(Level.INFO, message);
  }

  default void log(String message) {
    workflow().runner().log(Level.DEBUG, message);
  }

  default void run(ToolCall call) {
    workflow().runner().run(call);
  }
}
