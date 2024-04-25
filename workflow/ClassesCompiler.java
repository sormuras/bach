/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import run.bach.ToolCall;
import run.bach.workflow.Structure.Space;

/** Translate Java source files into class files. */
public interface ClassesCompiler extends Action {
  // TODO Replace with java.lang.ScopedValue of https://openjdk.org/jeps/464
  InheritableThreadLocal<Space> SPACE = new InheritableThreadLocal<>();

  private Space space() {
    return SPACE.get();
  }

  default void compileClasses(Space space) {
    if (SPACE.get() != null) throw new IllegalStateException();
    try {
      SPACE.set(space);
      var javac = classesCompilerUsesJavacToolCall();
      javac = classesCompilerWithRelease(javac);
      javac = classesCompilerWithModules(javac);
      javac = classesCompilerWithModuleSourcePaths(javac);
      javac = classesCompilerWithModulePaths(javac);
      javac = classesCompilerWithModulePatches(javac);
      javac = classesCompilerWithEncoding(javac);
      javac = classesCompilerWithDestinationDirectory(javac);
      classesCompilerRunJavacToolCall(javac);
    } finally {
      SPACE.remove();
    }
  }

  default ToolCall classesCompilerUsesJavacToolCall() {
    return ToolCall.of("javac");
  }

  default ToolCall classesCompilerWithRelease(ToolCall javac) {
    return space().targets().map(feature -> javac.add("--release", feature)).orElse(javac);
  }

  default ToolCall classesCompilerWithModules(ToolCall javac) {
    return javac.add("--module", space().modules().names(","));
  }

  default ToolCall classesCompilerWithModuleSourcePaths(ToolCall javac) {
    for (var moduleSourcePath : space().modules().toModuleSourcePaths()) {
      javac = javac.add("--module-source-path", moduleSourcePath);
    }
    return javac;
  }

  default ToolCall classesCompilerWithModulePaths(ToolCall javac) {
    var modulePath = space().toModulePath(workflow().folders());
    if (modulePath.isPresent()) {
      javac = javac.add("--module-path", modulePath.get());
      javac = javac.add("--processor-module-path", modulePath.get());
    }
    return javac;
  }

  default ToolCall classesCompilerWithModulePatches(ToolCall javac) {
    var spaces = workflow().structure().spaces();
    var folders = workflow().folders();
    for (var declaration : space().modules().list()) {
      var module = declaration.name();
      var patches = new ArrayList<String>();
      for (var requires : space().requires()) {
        if (spaces.space(requires).modules().find(module).isEmpty()) {
          continue;
        }
        patches.add(folders.out(requires, "modules", module + ".jar").toString());
      }
      if (patches.isEmpty()) continue;
      var patch = String.join(File.pathSeparator, patches);
      javac = javac.add("--patch-module", module + "=" + patch);
    }
    return javac;
  }

  default ToolCall classesCompilerWithEncoding(ToolCall javac) {
    return javac.add("-encoding", space().encoding().name());
  }

  default Path classesCompilerUsesDestinationDirectory() {
    var folders = workflow().folders();
    var feature = space().targets().orElse(Runtime.version().feature());
    return folders.out(space().name(), "classes").resolve("java-" + feature);
  }

  default ToolCall classesCompilerWithDestinationDirectory(ToolCall javac) {
    var classes = classesCompilerUsesDestinationDirectory();
    return javac.add("-d", classes);
  }

  default void classesCompilerRunJavacToolCall(ToolCall javac) {
    run(javac);
  }
}
