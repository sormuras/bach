/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import run.bach.workflow.Structure.Space;
import run.bach.workflow.Structure.Spaces;

/** Translates source files into modular JAR files. */
public interface Compiler extends Action, ClassesCompiler, ModulesCompiler {
  /** Translates source files into modular JAR files for all module spaces. */
  default void compile() {
    var spaces = compilerUsesSpaces();
    var names = spaces.names();
    if (names.isEmpty()) {
      say("No module space declared; nothing to compile.");
      return;
    }
    var size = names.size();
    say("Compiling %d module space%s %s ...".formatted(size, size == 1 ? "" : "s", names));
    spaces.forEach(this::compile);
  }

  /** Translates source files into modular JAR files for the specified module space. */
  default void compile(Space space) {
    var name = space.name();
    var size = space.modules().list().size();
    if (size == 0) {
      log("No modules declared in %s space.".formatted(name));
      return;
    }
    log("Compiling %d module%s in %s space...".formatted(size, size == 1 ? "" : "s", name));
    compileClasses(space);
    compileModules(space);
  }

  default Spaces compilerUsesSpaces() {
    return workflow().structure().spaces();
  }
}
