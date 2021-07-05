package com.github.sormuras.bach.project;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record ModuleSourcePaths(List<String> patterns, Map<String, List<Path>> specifics) {

  public static final ModuleSourcePaths EMPTY = new ModuleSourcePaths(List.of(), Map.of());

  public static ModuleSourcePaths ofPatterns(String... patterns) {
    return new ModuleSourcePaths(List.of(patterns), Map.of());
  }
}
