/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.util.Optional;
import run.bach.ToolCall;
import run.bach.workflow.Structure.Space;

public interface Starter extends Action {
  // TODO Replace with java.lang.ScopedValue of https://openjdk.org/jeps/464
  InheritableThreadLocal<Space> SPACE = new InheritableThreadLocal<>();

  static Space space() {
    return Optional.ofNullable(SPACE.get()).orElseThrow(IllegalStateException::new);
  }

  default void start(String... args) {
    var description = starterUsesProjectDescription();
    say("Starting %s ...".formatted(description));
    var space = starterUsesSpaceForStartingAnApplication();
    start(space, args);
  }

  default void start(Space space, String... args) {
    if (SPACE.get() != null) throw new IllegalStateException();
    try {
      SPACE.set(space);
      var java = starterUsesJavaToolCall();
      java = starterWithModulePath(java);
      java = starterWithModule(java);
      java = starterWithArguments(java, args);
      starterRunJavaToolCall(java);
    } finally {
      SPACE.remove();
    }
  }

  default String starterUsesProjectDescription() {
    return workflow().structure().toNameAndVersion();
  }

  default Space starterUsesSpaceForStartingAnApplication() {
    return workflow().structure().spaces().space("main");
  }

  default ToolCall starterUsesJavaToolCall() {
    return ToolCall.of("java");
  }

  default ToolCall starterWithModulePath(ToolCall java) {
    var folders = workflow().folders();
    var modulePath = space().toRuntimeSpace().toModulePath(folders).orElse(".");
    return java.add("--module-path", modulePath);
  }

  default ToolCall starterWithModule(ToolCall java) {
    var space = space();
    var name = space.name();
    var launchers = space.launchers();
    if (launchers.isEmpty())
      throw new IllegalStateException("No launcher defined in %s space".formatted(name));
    if (launchers.size() > 1) {
      log("Multiple launcher declared in %s space - using first of: %s".formatted(name, launchers));
    }
    return java.add("--module", launchers.getFirst().toModuleAndMainClass());
  }

  default ToolCall starterWithArguments(ToolCall java, String... args) {
    return java.addAll(args);
  }

  default void starterRunJavaToolCall(ToolCall java) {
    workflow().runner().run(java);
  }
}
