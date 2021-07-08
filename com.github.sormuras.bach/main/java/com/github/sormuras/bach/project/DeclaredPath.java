package com.github.sormuras.bach.project;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

public record DeclaredPath(Set<Modifier> modifiers, Path path, Optional<JavaRelease> release) {

  public enum Modifier {
    SOURCE,
    RESOURCE
  }

  public static DeclaredPath of(Path path) {
    return new DeclaredPath(Set.of(Modifier.SOURCE), path, Optional.empty());
  }
}
