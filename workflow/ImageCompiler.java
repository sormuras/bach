/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.util.Optional;
import run.bach.ToolCall;
import run.bach.workflow.Structure.Space;

/** Assemble and optimize a set of modules and their dependencies into a custom runtime image. */
public interface ImageCompiler extends Action {
  default void compileImage(Space space) {
    var jlink = imageCompilerNewJLinkToolCall();
    jlink = imageCompilerWithOutputDirectory(jlink, space);
    jlink = imageCompilerWithLauncher(jlink, space);
    jlink = imageCompilerWithModules(jlink, space);
    jlink = imageCompilerWithModulePath(jlink, space);
    imageCompilerRunJLinkToolCall(jlink);
  }

  default ToolCall imageCompilerWithModulePath(ToolCall jlink, Space space) {
    var folders = workflow().folders();
    var modulePath = space.toRuntimeSpace().toModulePath(folders).orElse(".");
    return jlink.add("--module-path", modulePath);
  }

  default ToolCall imageCompilerWithModules(ToolCall jlink, Space space) {
    return jlink.add("--add-modules", space.modules().names(","));
  }

  default ToolCall imageCompilerNewJLinkToolCall() {
    return ToolCall.of("jlink");
  }

  default void imageCompilerRunJLinkToolCall(ToolCall jlink) {
    run(jlink);
  }

  default ToolCall imageCompilerWithOutputDirectory(ToolCall jlink, Space space) {
    var directory = workflow().folders().out(space.name(), "image");
    return jlink.add("--output", directory);
  }

  default ToolCall imageCompilerWithLauncher(ToolCall jlink, Space space) {
    var launcher = imageCompilerUsesLauncher(space);
    return launcher.map(string -> jlink.add("--launcher", string)).orElse(jlink);
  }

  default Optional<String> imageCompilerUsesLauncher(Space space) {
    var launchers = space.launchers();
    if (launchers.isEmpty()) return Optional.empty();
    if (launchers.size() > 1) log("Multiple %s launchers: %s".formatted(space.name(), launchers));
    return Optional.of(launchers.getFirst().toNameAndModuleAndMainClass());
  }
}
