/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.util.List;
import run.bach.ToolCall;
import run.bach.workflow.Structure.Launcher;
import run.bach.workflow.Structure.Space;

public interface JavaTester extends Action {
  // TODO Replace with java.lang.ScopedValue of https://openjdk.org/jeps/464
  InheritableThreadLocal<Space> SPACE = new InheritableThreadLocal<>();

  private Space space() {
    return SPACE.get();
  }

  default void testViaJava(Space space) {
    if (SPACE.get() != null) throw new IllegalStateException();
    try {
      SPACE.set(space);
      var launchers = javaTesterUsesLaunchersForTesting();
      for (var launcher : launchers) {
        var entry = launcher.toModuleAndMainClass();
        say("Testing via starting Java for launcher %s ...".formatted(entry));
        testViaJava(launcher);
      }
    } finally {
      SPACE.remove();
    }
  }

  default void testViaJava(Launcher launcher) {
    var java = javaTesterUsesJavaToolCall();
    java = javaTesterWithModulePath(java);
    java = javaTesterWithEnableAssertions(java);
    java = javaTesterWithModule(java, launcher);
    javaTesterRunJavaToolCall(java);
  }

  default List<Launcher> javaTesterUsesLaunchersForTesting() {
    return space().launchers();
  }

  default ToolCall javaTesterUsesJavaToolCall() {
    return ToolCall.of("java");
  }

  default ToolCall javaTesterWithEnableAssertions(ToolCall java) {
    return java.add("-ea"); // --enable-assertions
  }

  default ToolCall javaTesterWithModulePath(ToolCall java) {
    var folders = workflow().folders();
    var path = space().toRuntimeSpace().toModulePath(folders).orElse(".");
    return java.add("--module-path", path);
  }

  default ToolCall javaTesterWithModule(ToolCall java, Launcher launcher) {
    return java.add("--module", launcher.toModuleAndMainClass());
  }

  default void javaTesterRunJavaToolCall(ToolCall java) {
    run(java);
  }
}
