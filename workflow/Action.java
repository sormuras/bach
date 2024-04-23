/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.lang.System.Logger.Level;
import run.bach.ToolCall;

/**
 * An executable abstraction.
 *
 * <p>For basic naming and method signature conventions see the following {@code Maker}-{@code
 * make()} example.
 *
 * <pre><code>
 * interface Maker extends Action {
 *   default void make() {
 *     say("Making %s ...".formatted(makerUsesStringToDescribeIt()));
 *     try {
 *       Thread.sleep(makerUsesDurationToMakeIt());
 *     } catch (InterruptedException ignore) {}
 *     if (makerDoesBidFarewell()) {
 *       say("Finally made it. Bye!");
 *     }
 *   }
 *   default String makerUsesStringToDescribeIt() { return "something"; }
 *   default Duration makerUsesDurationToMakeIt() { return Duration.ZERO; }
 *   default boolean makerDoesBidFarewell() { return true; }
 * }
 * </code></pre>
 */
@FunctionalInterface
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
