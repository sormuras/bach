/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

public interface Cleaner extends Action {
  default void clean() {
    // rm -rf folders().out()
  }
}
