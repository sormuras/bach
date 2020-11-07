package com.github.sormuras.bach;

import com.github.sormuras.bach.module.ModuleDirectory;
import com.github.sormuras.bach.module.ModuleSearcher;
import java.net.URI;

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
    var linked = directory.links().get(module);
    var uri = linked != null ? URI.create(linked.uri()) : lookup.search(module).orElseThrow();
    httpCopy(uri, directory.path().resolve(module + ".jar"));
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
      for (var module : missing) {
        var optionalURI = searcher.search(module);
        var uri = optionalURI.orElseThrow(() -> new Error("Search failed for: " + module));
        httpCopy(uri, directory.path().resolve(module + ".jar"));
      }
    }
  }
}
