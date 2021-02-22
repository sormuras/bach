package com.github.sormuras.bach.project;

import java.lang.module.ModuleDescriptor.Version;

/** Fundamental project properties. */
public record Settings(Folders folders, String name, Version version) {

  public static Settings of(String root, String name, String version) {
    return new Settings(Folders.of(root), name, Version.parse(version));
  }

  public Settings folders(String home) {
    return new Settings(Folders.of(home), name, version);
  }

  public Settings name(String name) {
    return new Settings(folders, name, version);
  }

  public Settings version(String version) {
    return version(Version.parse(version));
  }

  public Settings version(Version version) {
    return new Settings(folders, name, version);
  }
}
