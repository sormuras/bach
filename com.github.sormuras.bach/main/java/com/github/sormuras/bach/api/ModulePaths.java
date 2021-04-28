package com.github.sormuras.bach.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record ModulePaths(List<Path> list) {
  public List<Path> pruned() {
    return list.stream().filter(Files::exists).toList();
  }
}
