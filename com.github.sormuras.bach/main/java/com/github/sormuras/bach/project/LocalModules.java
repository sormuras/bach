package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.Strings;
import java.io.File;
import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A map of local modules using their names as keys.
 *
 * @param map the map of local modules
 */
public record LocalModules(Map<String, LocalModule> map) implements ModuleFinder {

  @Override
  public Optional<ModuleReference> find(String name) {
    return findDeclaration(name).map(LocalModule::reference);
  }

  @Override
  public Set<ModuleReference> findAll() {
    return map.values().stream().map(LocalModule::reference).collect(Collectors.toSet());
  }

  Optional<LocalModule> findDeclaration(String name) {
    return Optional.ofNullable(map.get(name));
  }

  /** @return {@code true} if no module declaration is available, else {@code false} */
  public boolean isEmpty() {
    return map.isEmpty();
  }

  /** @return the number of module declarations available */
  public int size() {
    return map.size();
  }

  /** @return a sorted stream of all module names */
  public Stream<String> toNames() {
    return map.keySet().stream().sorted();
  }

  /**
   * Returns a string of all module names joined by the given delimiter.
   *
   * @param delimiter the delimiter
   * @return a string of all module names joined by the given delimiter
   */
  public String toNames(String delimiter) {
    return toNames().collect(Collectors.joining(delimiter));
  }

  /**
   * Returns a list of module source path forms.
   *
   * @param forceModuleSpecificForm if only module-specific should be generated
   * @return a list of module source path forms
   */
  public List<String> toModuleSourcePaths(boolean forceModuleSpecificForm) {
    var paths = new ArrayList<String>();
    var patterns = new TreeSet<String>(); // "src:etc/*/java"
    var specific = new TreeMap<String, List<Path>>(); // "foo=java:java-9"
    for (var unit : map.values()) {
      var sourcePaths = unit.sources().toModuleSpecificSourcePaths();
      if (forceModuleSpecificForm) {
        specific.put(unit.name(), sourcePaths);
        continue;
      }
      try {
        for (var path : sourcePaths) {
          patterns.add(Strings.toModuleSourcePathPatternForm(path, unit.name()));
        }
      } catch (FindException e) {
        specific.put(unit.name(), sourcePaths);
      }
    }
    if (patterns.isEmpty() && specific.isEmpty()) throw new IllegalStateException("No forms?!");
    if (!patterns.isEmpty()) paths.add(String.join(File.pathSeparator, patterns));
    var entries = specific.entrySet();
    for (var entry : entries) paths.add(entry.getKey() + "=" + Strings.join(entry.getValue()));
    return List.copyOf(paths);
  }

  /**
   * Return a map of module-paths entries.
   *
   * @param upstream the upstream module declarations
   * @return a map of module-paths entries each usable as a {@code --patch-module} value
   */
  public Map<String, String> toModulePatches(LocalModules upstream) {
    if (map.isEmpty() || upstream.isEmpty()) return Map.of();
    var patches = new TreeMap<String, String>();
    for (var declaration : map.values()) {
      var module = declaration.name();
      upstream
          .findDeclaration(module)
          .ifPresent(up -> patches.put(module, up.sources().toModuleSpecificSourcePath()));
    }
    return patches;
  }
}
