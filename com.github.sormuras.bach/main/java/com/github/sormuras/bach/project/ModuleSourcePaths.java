package com.github.sormuras.bach.project;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record ModuleSourcePaths(List<String> patterns, Map<String, List<Path>> specifics) {

  public static ModuleSourcePaths ofPatterns(String... patterns) {
    return new ModuleSourcePaths(List.of(patterns), Map.of());
  }

  public static ModuleSourcePaths of(DeclaredModules modules) {
    var specifics = new TreeMap<String, List<Path>>();
    for (var module : modules.set())
      specifics.put(module.name(), List.of(module.path().getParent()));
    return new ModuleSourcePaths(List.of(), specifics);
  }

  public ModuleSourcePaths {
    if (patterns.isEmpty() && specifics.isEmpty()) throw new IllegalArgumentException();
  }
}
