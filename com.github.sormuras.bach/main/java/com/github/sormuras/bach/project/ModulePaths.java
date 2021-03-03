package com.github.sormuras.bach.project;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** A module-path record. */
public record ModulePaths(List<Path> list) {

  public static ModulePaths of(Path root, String... paths) {
    return new ModulePaths(Arrays.stream(paths).map(root::resolve).toList());
  }
}
