/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import static java.lang.System.Logger.Level.*;

import java.util.stream.Stream;
import run.bach.ToolCall;
import run.bach.workflow.Structure.Space;

public interface Launcher extends Action {
  default void launch(String... args) {
    var main = workflow().structure().spaces().space("main");
    launch(main, args);
  }

  default void launch(Space space, String... args) {
    var logger = System.getLogger(Launcher.class.getName());
    var launchers = space.launchers();
    if (launchers.isEmpty()) {
      logger.log(ERROR, "No launcher defined. No launch.");
      return;
    }
    if (launchers.size() > 1) {
      logger.log(DEBUG, "Using first launcher of: " + launchers);
    }
    var launcher = launchers.getFirst();
    var folders = workflow().folders();
    var modulePath = space.toRuntimeSpace().toModulePath(folders).orElse(".");
    var java =
        ToolCall.of("java")
            .add("--module-path", modulePath)
            .add("--module", launcher)
            .addAll(Stream.of(args));
    workflow().runner().run(java);
  }
}
