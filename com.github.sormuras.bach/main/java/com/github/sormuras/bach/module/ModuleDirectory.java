package com.github.sormuras.bach.module;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.internal.Modules;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/** A collection of module-uri links and local asset management.
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
  public static ModuleDirectory of(Path path, ModuleLink... links) {
    var map = Arrays.stream(links).collect(toUnmodifiableMap(ModuleLink::module, identity()));
    return new ModuleDirectory(path, map);
  }

  /**
   * @param module to module to scan for link annotations
   * @return a new module directory with all module links associated with the given module
   */
  public ModuleDirectory withLinks(Module module) {
    var project = module.getAnnotation(Project.class);
    if (project == null) return this;
    var links = Set.of(project.links());
    if (links.isEmpty()) return this;

    var copy = new HashMap<>(this.links);
    links.forEach(link -> copy.put(link.module(), ModuleLink.of(link)));
    return new ModuleDirectory(path, Map.copyOf(copy));
  }

  /**
   * @param link the new module link to register
   * @param more more module links to register
   * @return a new module directory
   */
  public ModuleDirectory withLinks(ModuleLink link, ModuleLink... more) {
    var copy = new HashMap<>(links);
    copy.put(link.module(), link);
    Arrays.stream(more).forEach(next -> copy.put(next.module(), next));
    return new ModuleDirectory(path, Map.copyOf(copy));
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
  public Optional<URI> lookup(String module) {
    return Optional.ofNullable(links.get(module)).map(ModuleLink::uri).map(URI::create);
  }

  /** @return the names of all modules that are required but not locatable by this instance */
  public Set<String> missing() {
    var finder = finder();
    var missing = new HashSet<>(Modules.required(finder));
    if (missing.isEmpty()) return Set.of();
    missing.removeAll(Modules.declared(finder));
    missing.removeAll(Modules.declared(ModuleFinder.ofSystem()));
    if (missing.isEmpty()) return Set.of();
    return missing;
  }
}
