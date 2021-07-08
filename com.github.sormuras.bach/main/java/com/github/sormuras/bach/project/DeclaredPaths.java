package com.github.sormuras.bach.project;

import java.nio.file.Path;
import java.util.List;

public record DeclaredPaths(List<DeclaredPath> list) {
  public List<Path> list(int release) {
    return list.stream()
        .filter(path -> path.release().map(JavaRelease::getAsInt).orElse(0) == release)
        .map(DeclaredPath::path)
        .toList();
  }
}
