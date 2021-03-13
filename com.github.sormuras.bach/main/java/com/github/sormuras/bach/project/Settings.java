package com.github.sormuras.bach.project;

import java.lang.module.ModuleDescriptor.Version;

/**
 * Fundamental project properties.
 *
 * @param name the name of the project
 * @param version the version of the project
 * @param folders well-known directories
 * @param tools tool-related settings
 */
public record Settings(String name, Version version, Folders folders, Tools tools) {

  /** {@return a {@code Settings} instance based on the given components} */
  public static Settings of(String root, String name, String version, Tools tools) {
    return new Settings(name, Version.parse(version), Folders.of(root), tools);
  }

  /** {@return a copy of this instance with the given component replaced} */
  public Settings folders(String root) {
    return new Settings(name, version, Folders.of(root), tools);
  }

  /** {@return a copy of this instance with the given component replaced} */
  public Settings name(String name) {
    return new Settings(name, version, folders, tools);
  }

  /** {@return a copy of this instance with the given component replaced} */
  public Settings version(String version) {
    return version(Version.parse(version));
  }

  /** {@return a copy of this instance with the given component replaced} */
  public Settings version(Version version) {
    return new Settings(name, version, folders, tools);
  }
}
