package com.github.sormuras.bach.module;

import com.github.sormuras.bach.internal.Modules;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A collection of module-uri links and local asset management.
 *
 * @param path the directory module are stored in
 * @param links the module-uri pairs
 */
public record ModuleDirectory(Path path, Map<String, ModuleLink> links) {

  /**
   * @param path the directory module are stored in
   * @param links the module-uri pairs
   * @return a library for the given directory and links
   */
  public static ModuleDirectory of(Path path, Map<String, String> links) {
    var map = new HashMap<String, ModuleLink>();
    for(var link : links.entrySet())
      map.put(link.getKey(), new ModuleLink(link.getKey(), link.getValue()));
    return new ModuleDirectory(path, Map.copyOf(map));
  }

  /** @return a new stream of module-uri pairs */
  public Stream<ModuleLink> stream() {
    return links().values().stream();
  }

  /** @return a new module finder using the path of this module directory */
  public ModuleFinder finder() {
    return ModuleFinder.of(path);
  }

  /**
   * @param module the name of the module
   * @return an optional with URI created from the registered module links or an empty optional
   * @see ModuleSearcher
   */
  public Optional<String> lookup(String module) {
    return Optional.ofNullable(links.get(module)).map(ModuleLink::uri);
  }

  /**
   * @param module the name of the module
   * @param searcher the searcher of remote modules
   * @return a uri targeting a remote modular JAR file
   */
  public URI lookup(String module, ModuleSearcher searcher) {
    return lookup(module)
        .or(() -> searcher.search(module))
        .map(URI::create)
        .orElseThrow(() -> new RuntimeException("Module not found: " + module));
  }

  /**
   * @param module the name of the module
   * @return a path pointing to the modular JAR file of the given module
   */
  public Path jar(String module) {
    return path.resolve(module + ".jar");
  }

  /** @return the names of all modules that are required but not locatable by this instance */
  public Set<String> missing() {
    var finder = finder();
    var missing = Modules.required(finder); // as mutable TreeSet<String>
    if (missing.isEmpty()) return Set.of();
    missing.removeAll(Modules.declared(finder));
    missing.removeAll(Modules.declared(ModuleFinder.ofSystem()));
    if (missing.isEmpty()) return Set.of();
    return missing;
  }
}
