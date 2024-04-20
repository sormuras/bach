/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

public interface Builder extends Action, Cleaner, Compiler, Tester {
  default void build() {
    if (builderShouldInvokeCleanBeforeCompile()) {
      clean();
    }
    compile();
    test();
  }

  default boolean builderShouldInvokeCleanBeforeCompile() {
    return false;
  }
}
