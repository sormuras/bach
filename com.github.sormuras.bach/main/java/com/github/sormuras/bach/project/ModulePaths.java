package com.github.sormuras.bach.project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public record ModulePaths(List<Path> list) {
  public static ModulePaths of(String... paths) {
    return new ModulePaths(Stream.of(paths).map(Path::of).toList());
  }

  public List<Path> pruned() {
    return list.stream().filter(Files::exists).toList();
  }
}
