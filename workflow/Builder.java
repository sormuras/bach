/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

public interface Builder extends Action, Cleaner, Compiler, Restorer, Tester {
  default void build() {
    var description = builderUsesProjectDescription();
    say("Building %s ...".formatted(description));
    if (builderDoesCleanAtTheBeginning()) {
      clean();
    }
    restore();
    compile();
    test();
    say("Build of %s completed.".formatted(description));
  }

  default String builderUsesProjectDescription() {
    return workflow().structure().toNameAndVersion();
  }

  default boolean builderDoesCleanAtTheBeginning() {
    return false;
  }
}
