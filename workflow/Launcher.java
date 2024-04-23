/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import run.bach.ToolCall;
import run.bach.workflow.Structure.Space;

public interface Launcher extends Action {
  default void launch(String... args) {
    var description = launcherUsesProjectDescription();
    say("Launching %s ...".formatted(description));
    var space = launcherUsesSpaceForLaunchingTheApplication();
    launch(space, args);
  }

  default void launch(Space space, String... args) {
    var java = launcherUsesJavaToolCall();
    java = launcherWithModulePath(java, space);
    java = launcherWithModule(java, space);
    java = launcherWithArguments(java, args);
    launcherRunJavaToolCall(java);
  }

  default String launcherUsesProjectDescription() {
    return workflow().structure().toNameAndVersion();
  }

  default Space launcherUsesSpaceForLaunchingTheApplication() {
    return workflow().structure().spaces().space("main");
  }

  default ToolCall launcherUsesJavaToolCall() {
    return ToolCall.of("java");
  }

  default ToolCall launcherWithModulePath(ToolCall java, Space space) {
    var folders = workflow().folders();
    var modulePath = space.toRuntimeSpace().toModulePath(folders).orElse(".");
    return java.add("--module-path", modulePath);
  }

  default ToolCall launcherWithModule(ToolCall java, Space space) {
    var name = space.name();
    var launchers = space.launchers();
    if (launchers.isEmpty())
      throw new IllegalStateException("No launcher defined in %s space".formatted(name));
    if (launchers.size() > 1) {
      log("Multiple launcher declared in %s space - using first of: %s".formatted(name, launchers));
    }
    return java.add("--module", launchers.getFirst().toModuleAndMainClass());
  }

  default ToolCall launcherWithArguments(ToolCall java, String... args) {
    return java.addAll(args);
  }

  default void launcherRunJavaToolCall(ToolCall java) {
    workflow().runner().run(java);
  }
}
