package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Strings;
import java.lang.module.ModuleDescriptor.Version;

public record Project(String name, Version version, Libraries libraries) {

  public Project() {
    this("noname", Version.parse("0"), Libraries.of());
  }

  public Project name(String name) {
    return new Project(name, version, libraries);
  }

  public Project version(String version) {
    return version(Version.parse(version));
  }

  public Project version(Version version) {
    return new Project(name, version, libraries);
  }

  public Project libraries(Libraries libraries) {
    return new Project(name, version, libraries);
  }

  public String toVersionNumberAndPreRelease() {
    return Strings.toNumberAndPreRelease(version);
  }
}
