package com.github.sormuras.bach.project;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.internal.Modules;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * An external modules configuration.
 *
 * @param requires the set of required modules
 * @param links the module-uri pairs
 */
public record ExternalModules(Set<String> requires, Map<String, ExternalModule> links)
    implements ModuleLookup {

  /** @return a new stream of module-uri pairs */
  public Stream<ExternalModule> stream() {
    return links().values().stream();
  }

  /** @return a new module finder using the path of this module directory */
  public ModuleFinder finder() {
    return ModuleFinder.of(Bach.EXTERNALS);
  }

  @Override
  public Optional<String> lookup(String module) {
    return Optional.ofNullable(links.get(module)).map(ExternalModule::uri);
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
