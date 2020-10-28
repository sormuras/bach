package com.github.sormuras.bach;

import com.github.sormuras.bach.module.ModuleDirectory;
import com.github.sormuras.bach.module.ModuleLookup;

/** Module loading-related API. */
public /*sealed*/ interface Load extends Http /*permits Bach*/ {

  /**
   * Load a module.
   *
   * @param directory the module finder to query for already loaded modules
   * @param lookup the function that maps a module name to its uri
   * @param module the name of the module to load
   */
  default void loadModule(ModuleDirectory directory, ModuleLookup lookup, String module) {
    if (directory.finder().find(module).isPresent()) return;
    var uri = lookup.find(module).orElseThrow();
    httpCopy(uri, directory.path().resolve(module + ".jar"));
  }
}
