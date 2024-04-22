/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

public interface Builder extends Action, Cleaner, Compiler, Restorer, Tester {
  default void build() {
    var project = workflow().structure().toNameAndVersion();
    say("Building %s ...".formatted(project));
    if (builderShouldInvokeCleanBeforeCompile()) {
      clean();
    }
    restore();
    compile();
    test();
    say("Build of %s completed.".formatted(project));
  }

  default boolean builderShouldInvokeCleanBeforeCompile() {
    return false;
  }
}
