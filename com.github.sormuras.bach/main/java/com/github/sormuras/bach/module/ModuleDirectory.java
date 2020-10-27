package com.github.sormuras.bach.module;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

/** A collection of module-uri links and local asset management. */
public final class ModuleDirectory {

  /**
   * @param path the directory module are stored in
   * @param links the module-uri pairs
   * @return a library for the given directory and links
   */
  public static ModuleDirectory of(Path path, ModuleLink... links) {
    var map = Arrays.stream(links).collect(toUnmodifiableMap(ModuleLink::module, identity()));
    return new ModuleDirectory(path, map);
  }

  private final Path path;
  private final Map<String, ModuleLink> links;

  /**
   * Initialize a library with the given components.
   *
   * @param path the directory module are stored in
   * @param links the module-uri pairs
   */
  public ModuleDirectory(Path path, Map<String, ModuleLink> links) {
    this.path = path;
    this.links = links;
  }

  /** @return the directory module are stored in */
  public Path path() {
    return path;
  }

  /** @return the registered module-uri pairs */
  public Map<String, ModuleLink> links() {
    return links;
  }

  /** @return a new stream of module-uri pairs */
  public Stream<ModuleLink> stream() {
    return links().values().stream();
  }
}
