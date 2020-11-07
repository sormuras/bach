package com.github.sormuras.bach;

import com.github.sormuras.bach.module.ModuleDirectory;
import com.github.sormuras.bach.module.ModuleSearcher;

/** Module loading-related API. */
public /*sealed*/ interface Load extends Http /*permits Bach*/ {

  /**
   * Load a module.
   *
   * @param directory the module finder to query for already loaded modules
   * @param searcher the function that maps a module name to its uri
   * @param module the name of the module to load
   */
  default void loadModule(ModuleDirectory directory, ModuleSearcher searcher, String module) {
    if (directory.finder().find(module).isPresent()) return;
    httpCopy(directory.lookup(module, searcher), directory.jar(module));
  }

  /**
   * Load all missing modules of the given module directory.
   *
   * @param directory the module finder to query for already loaded modules
   * @param searcher the searcher to query for linked module-
   */
  default void loadMissingModules(ModuleDirectory directory, ModuleSearcher searcher) {
    while (true) {
      var missing = directory.missing();
      if (missing.isEmpty()) return;
      for (var module : missing) httpCopy(directory.lookup(module, searcher), directory.jar(module));
    }
  }
}
