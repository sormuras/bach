package com.github.sormuras.bach.project;

import java.nio.file.Path;

/**
 * A module declaration connects a module compilation unit with source folders and resource folders.
 *
 * @param root the content root directory
 * @param reference the module info reference
 * @param sources the source folders
 * @param resources the resource folders
 */
public record ModuleDeclaration(
    Path root, ModuleInfoReference reference, SourceFolders sources, SourceFolders resources)
    implements Comparable<ModuleDeclaration> {

  @Override
  public int compareTo(ModuleDeclaration other) {
    return name().compareTo(other.name());
  }

  /** {@return the module name} */
  public String name() {
    return reference.name();
  }
}
