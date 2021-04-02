package com.github.sormuras.bach.project;

import java.nio.file.Path;

/**
 * A project-local module record connects a module compilation unit with source folders and resource
 * folders.
 *
 * @param root the content root directory
 * @param reference the module info reference
 * @param sources the source folders
 * @param resources the resource folders
 */
public record LocalModule(
    Path root, ModuleInfoReference reference, SourceFolders sources, SourceFolders resources)
    implements Comparable<LocalModule> {

  @Override
  public int compareTo(LocalModule other) {
    return name().compareTo(other.name());
  }

  /** {@return the module name} */
  public String name() {
    return reference.name();
  }
}
