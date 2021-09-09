package com.github.sormuras.bach.project;

import java.nio.file.Path;

/**
 * Targets a folder to a Java feature version, with {@code 0} indicating no target version.
 *
 * @param directory the directory
 * @param version the Java feature release version to target
 * @param types the set of types associated with the directory
 */
public record TargetedFolder(Path directory, int version, FolderTypes types)
    implements Comparable<TargetedFolder> {

  public TargetedFolder with(Object component) {
    return new TargetedFolder(
        component instanceof Path directory ? directory : directory,
        component instanceof Integer version ? version : version,
        component instanceof FolderTypes types ? types : types);
  }

  @Override
  public int compareTo(TargetedFolder other) {
    return version - other.version;
  }
}
