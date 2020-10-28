package com.github.sormuras.bach;

import com.github.sormuras.bach.module.ModuleDirectory;
import com.github.sormuras.bach.module.ModuleSearcher;

/** Module loading-related API. */
public /*sealed*/ interface Load extends Http /*permits Bach*/ {

  /**
   * Load a module.
   *
   * @param directory the module finder to query for already loaded modules
   * @param lookup the function that maps a module name to its uri
   * @param module the name of the module to load
   */
  default void loadModule(ModuleDirectory directory, ModuleSearcher lookup, String module) {
    if (directory.finder().find(module).isPresent()) return;
    var uri = lookup.search(module).orElseThrow();
    httpCopy(uri, directory.path().resolve(module + ".jar"));
  }

  /**
   * Load all missing modules of the given module directory.
   *
   * @param directory the module finder to query for already loaded modules
   * @param lookup the function that maps a module name to its uri
   */
  default void loadMissingModules(ModuleDirectory directory, ModuleSearcher lookup) {
    while (true) {
      var missing = directory.missing();
      if (missing.isEmpty()) return;
      for (var module : missing) {
        var uri = lookup.search(module).orElseThrow(() -> new Error("Lookup failed: " + module));
        httpCopy(uri, directory.path().resolve(module + ".jar"));
      }
    }
  }
}
