/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.io.File;
import java.util.ArrayList;
import run.bach.*;
import run.bach.workflow.Structure.Space;

/** Translate Java source files into class files. */
public interface ClassesCompiler extends Action {
  default void compileClasses(Space space) {
    var javac = compileClassesCreateJavacCall();
    javac = compileClassesWithRelease(javac, space);
    javac = compileClassesWithModules(javac, space);
    javac = compileClassesWithModuleSourcePaths(javac, space);
    javac = compileClassesWithModulePaths(javac, space);
    javac = compileClassesWithModulePatches(javac, space);
    javac = compileClassesWithDestinationDirectory(javac, space);
    workflow().runner().run(javac);
  }

  default ToolCall compileClassesCreateJavacCall() {
    return ToolCall.of("javac");
  }

  default ToolCall compileClassesWithRelease(ToolCall javac, Space space) {
    return space.targets().map(feature -> javac.add("--release", feature)).orElse(javac);
  }

  default ToolCall compileClassesWithModules(ToolCall javac, Space space) {
    return javac.add("--module", space.modules().names(","));
  }

  default ToolCall compileClassesWithModuleSourcePaths(ToolCall javac, Space space) {
    for (var moduleSourcePath : space.modules().toModuleSourcePaths()) {
      javac = javac.add("--module-source-path", moduleSourcePath);
    }
    return javac;
  }

  default ToolCall compileClassesWithModulePaths(ToolCall javac, Space space) {
    var modulePath = space.toModulePath(workflow().folders());
    if (modulePath.isPresent()) {
      javac = javac.add("--module-path", modulePath.get());
      javac = javac.add("--processor-module-path", modulePath.get());
    }
    return javac;
  }

  default ToolCall compileClassesWithModulePatches(ToolCall javac, Space space) {
    var spaces = workflow().structure().spaces();
    var folders = workflow().folders();
    for (var declaration : space.modules().list()) {
      var module = declaration.name();
      var patches = new ArrayList<String>();
      for (var requires : space.requires()) {
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

  default ToolCall compileClassesWithDestinationDirectory(ToolCall javac, Space space) {
    var folders = workflow().folders();
    var feature = space.targets().orElse(Runtime.version().feature());
    var classes = folders.out(space.name(), "classes").resolve("java-" + feature);
    return javac.add("-d", classes);
  }
}
