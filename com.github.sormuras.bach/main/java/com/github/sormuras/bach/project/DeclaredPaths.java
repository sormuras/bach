package com.github.sormuras.bach.project;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public record DeclaredPaths(List<DeclaredPath> list) {
  public List<Path> list(int release, PathType... types) {
    return list(release, Set.of(types));
  }

  public List<Path> list(int release, Set<PathType> types) {
    return list.stream()
        .filter(path -> path.release().map(JavaRelease::getAsInt).orElse(0) == release)
        .filter(path -> path.types().containsAll(types))
        .map(DeclaredPath::path)
        .toList();
  }
}
