/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import run.bach.workflow.Structure.Space;
import run.bach.workflow.Structure.Spaces;

/** Translates source files into modular JAR files. */
public interface Compiler extends Action, ClassesCompiler, ModulesCompiler, ImageCompiler {
  // TODO Replace with java.lang.ScopedValue of https://openjdk.org/jeps/464
  InheritableThreadLocal<Space> SPACE = new InheritableThreadLocal<>();

  private Space space() {
    return SPACE.get();
  }

  /** Translates source files into modular JAR files for all module spaces. */
  default void compile() {
    var spaces = compilerUsesSpacesForCompilation();
    var names = spaces.names();
    if (names.isEmpty()) {
      say("No module space declared; nothing to compile.");
      return;
    }
    var size = names.size();
    say("Compiling %d module space%s %s ...".formatted(size, size == 1 ? "" : "s", names));
    for (Space space : spaces) {
      compile(space);
    }
  }

  /** Translates source files into modular JAR files for the specified module space. */
  default void compile(Space space) {
    if (SPACE.get() != null) throw new IllegalStateException();
    try {
      SPACE.set(space);
      var name = space.name();
      var size = space.modules().list().size();
      if (size == 0) {
        log("No modules declared in %s space.".formatted(name));
        return;
      }
      log("Compiling %d module%s in %s space...".formatted(size, size == 1 ? "" : "s", name));
      compileClasses(space);
      compileModules(space);
      if (compilerDoesCreateCustomRuntimeImage()) {
        compileImage(space);
      }
    } finally {
      SPACE.remove();
    }
  }

  default Spaces compilerUsesSpacesForCompilation() {
    return workflow().structure().spaces();
  }

  default boolean compilerDoesCreateCustomRuntimeImage() {
    return space().is(Space.Flag.IMAGE);
  }
}
