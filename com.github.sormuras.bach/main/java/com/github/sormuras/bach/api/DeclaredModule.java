package com.github.sormuras.bach.api;

import java.nio.file.Path;
import java.util.List;

public record DeclaredModule(
    Path root, DeclaredModuleReference reference, SourceFolders sources, SourceFolders resources)
    implements Comparable<DeclaredModule> {

  @Override
  public int compareTo(DeclaredModule other) {
    return name().compareTo(other.name());
  }

  public String name() {
    return reference.name();
  }

  List<Path> toModuleSpecificSourcePaths() {
    return sources.list().isEmpty() ? List.of(root) : sources.toModuleSpecificSourcePaths();
  }
}
