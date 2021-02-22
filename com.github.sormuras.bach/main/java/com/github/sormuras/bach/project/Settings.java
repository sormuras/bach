package com.github.sormuras.bach.project;

import java.lang.module.ModuleDescriptor.Version;

/**
 * Fundamental project properties.
 *
 * @param folders well-known directories
 * @param name the name of the project
 * @param version the version of the project
 */
public record Settings(Folders folders, String name, Version version) {

  /** {@return a {@code Settings} instance based on the given components} */
  public static Settings of(String root, String name, String version) {
    return new Settings(Folders.of(root), name, Version.parse(version));
  }

  /** {@return a copy of this instance with the given component replaced} */
  public Settings folders(String root) {
    return new Settings(Folders.of(root), name, version);
  }

  /** {@return a copy of this instance with the given component replaced} */
  public Settings name(String name) {
    return new Settings(folders, name, version);
  }

  /** {@return a copy of this instance with the given component replaced} */
  public Settings version(String version) {
    return version(Version.parse(version));
  }

  /** {@return a copy of this instance with the given component replaced} */
  public Settings version(Version version) {
    return new Settings(folders, name, version);
  }
}
