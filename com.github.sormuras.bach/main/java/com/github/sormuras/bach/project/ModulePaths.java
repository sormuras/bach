package com.github.sormuras.bach.project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** A module-path record. */
public record ModulePaths(List<Path> list) {
  public List<Path> pruned() {
    return list.stream().filter(Files::exists).toList();
  }
}
