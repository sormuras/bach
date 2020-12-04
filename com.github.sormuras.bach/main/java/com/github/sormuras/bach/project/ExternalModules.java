package com.github.sormuras.bach.project;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.internal.Modules;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * An external modules configuration.
 *
 * @param requires the set of additionally required modules
 * @param links the module-uri pairs
 * @param lookups the list of module's uri lookups
 */
public record ExternalModules(Set<String> requires, Map<String, ExternalModule> links, List<ModuleLookup> lookups) {

  /** @return a new stream of module-uri pairs */
  public Stream<ExternalModule> stream() {
    return links().values().stream();
  }

  /** @return a new module finder using the path of this module directory */
  public ModuleFinder finder() {
    return ModuleFinder.of(Bach.EXTERNALS);
  }

  /**
   * @param module the name of the module
   * @return an optional with URI created from the registered module links or an empty optional
   * @see ModuleLookup
   */
  public Optional<String> lookup(String module) {
    return Optional.ofNullable(links.get(module)).map(ExternalModule::uri);
  }

  /**
   * @param module the name of the module
   * @param searcher the searcher of remote modules
   * @return a uri targeting a remote modular JAR file
   */
  public URI lookup(String module, ModuleLookup searcher) {
    return lookup(module)
        .or(() -> searcher.lookup(module))
        .map(URI::create)
        .orElseThrow(() -> new RuntimeException("Module not found: " + module));
  }

  /**
   * @param module the name of the module
   * @return a path pointing to the modular JAR file of the given module
   */
  public Path jar(String module) {
    return Bach.EXTERNALS.resolve(module + ".jar");
  }

  /** @return the names of all modules that are required but not locatable by this instance */
  public Set<String> missing() {
    var finder = finder();
    var missing = Modules.required(finder);
    if (missing.isEmpty()) return Set.of();
    missing.removeAll(Modules.declared(finder));
    missing.removeAll(Modules.declared(ModuleFinder.ofSystem()));
    if (missing.isEmpty()) return Set.of();
    return missing;
  }
}
