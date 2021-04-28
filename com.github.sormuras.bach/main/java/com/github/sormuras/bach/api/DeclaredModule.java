package com.github.sormuras.bach.api;

import java.nio.file.Path;

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
}
