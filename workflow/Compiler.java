/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import static java.lang.System.Logger.Level.*;

public interface Compiler extends Action, ClassesCompiler, ClassesPackager {
  default void compile() {
    var logger = System.getLogger(Compiler.class.getName());
    for (var space : workflow().structure().spaces()) {
      var name = space.name();
      var modules = space.modules().list();
      if (modules.isEmpty()) {
        logger.log(TRACE, "No modules declared in %s space.".formatted(name));
        continue;
      }
      var size = modules.size();
      var s = size == 1 ? "" : "s";
      logger.log(DEBUG, "Compile %d module%s in %s space...".formatted(size, s, name));
      compileClasses(space);
      packageClasses(space);
    }
  }
}
