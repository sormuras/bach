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
  // TODO Replace with java.lang.ScopedValue of https://openjdk.org/jeps/464
  InheritableThreadLocal<Space> SPACE = new InheritableThreadLocal<>();

  private Space space() {
    return SPACE.get();
  }

  default void compileImage(Space space) {
    if (SPACE.get() != null) throw new IllegalStateException();
    try {
      SPACE.set(space);
      var jlink = imageCompilerUsesJLinkToolCall();
      jlink = imageCompilerWithOutputDirectory(jlink);
      jlink = imageCompilerWithLauncher(jlink);
      jlink = imageCompilerWithModules(jlink);
      jlink = imageCompilerWithModulePath(jlink);
      imageCompilerRunJLinkToolCall(jlink);
    } finally {
      SPACE.remove();
    }
  }

  default ToolCall imageCompilerUsesJLinkToolCall() {
    return ToolCall.of("jlink");
  }

  default ToolCall imageCompilerWithModulePath(ToolCall jlink) {
    var folders = workflow().folders();
    var modulePath = space().toRuntimeSpace().toModulePath(folders).orElse(".");
    return jlink.add("--module-path", modulePath);
  }

  default ToolCall imageCompilerWithModules(ToolCall jlink) {
    return jlink.add("--add-modules", space().modules().names(","));
  }

  default void imageCompilerRunJLinkToolCall(ToolCall jlink) {
    run(jlink);
  }

  default ToolCall imageCompilerWithOutputDirectory(ToolCall jlink) {
    var directory = workflow().folders().out(space().name(), "image");
    return jlink.add("--output", directory);
  }

  default ToolCall imageCompilerWithLauncher(ToolCall jlink) {
    var launcher = imageCompilerUsesLauncher();
    return launcher.map(string -> jlink.add("--launcher", string)).orElse(jlink);
  }

  default Optional<String> imageCompilerUsesLauncher() {
    var space = space();
    var launchers = space.launchers();
    if (launchers.isEmpty()) return Optional.empty();
    if (launchers.size() > 1) log("Multiple %s launchers: %s".formatted(space.name(), launchers));
    return Optional.of(launchers.getFirst().toNameAndModuleAndMainClass());
  }
}
