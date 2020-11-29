package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.Paths;
import java.io.File;
import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.ArrayDeque;
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
 * A map of module declarations using their names as keys.
 *
 * @param map the map of module declarations
 */
public record ModuleDeclarations(Map<String, ModuleDeclaration> map) implements ModuleFinder {

  @Override
  public Optional<ModuleReference> find(String name) {
    return findDeclaration(name).map(ModuleDeclaration::reference);
  }

  @Override
  public Set<ModuleReference> findAll() {
    return map.values().stream().map(ModuleDeclaration::reference).collect(Collectors.toSet());
  }

  Optional<ModuleDeclaration> findDeclaration(String name) {
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
          patterns.add(toModuleSourcePathPatternForm(path, unit.name()));
        }
      } catch (FindException e) {
        specific.put(unit.name(), sourcePaths);
      }
    }
    if (patterns.isEmpty() && specific.isEmpty()) throw new IllegalStateException("No forms?!");
    if (!patterns.isEmpty()) paths.add(String.join(File.pathSeparator, patterns));
    var entries = specific.entrySet();
    for (var entry : entries) paths.add(entry.getKey() + "=" + Paths.join(entry.getValue()));
    return List.copyOf(paths);
  }

  /**
   * Return a map of module-paths entries.
   *
   * @param upstream the upstream module declarations
   * @return a map of module-paths entries each usable as a {@code --patch-module} value
   */
  public Map<String, String> toModulePatches(ModuleDeclarations upstream) {
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

  /** Return a string in module-pattern form usable as a {@code --module-source-path} value. */
  static String toModuleSourcePathPatternForm(Path info, String module) {
    var deque = new ArrayDeque<String>();
    for (var element : info.normalize()) {
      var name = element.toString();
      if (name.equals("module-info.java")) continue;
      deque.addLast(name.equals(module) ? "*" : name);
    }
    var pattern = String.join(File.separator, deque);
    if (!pattern.contains("*")) throw new FindException("Name '" + module + "' not found: " + info);
    if (pattern.equals("*")) return ".";
    if (pattern.endsWith("*")) return pattern.substring(0, pattern.length() - 2);
    if (pattern.startsWith("*")) return "." + File.separator + pattern;
    return pattern;
  }
}
