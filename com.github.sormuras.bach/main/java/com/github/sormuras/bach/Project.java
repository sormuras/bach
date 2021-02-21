package com.github.sormuras.bach;

import com.github.sormuras.bach.project.Libraries;
import com.github.sormuras.bach.project.Name;
import com.github.sormuras.bach.project.Spaces;
import com.github.sormuras.bach.project.Version;

public record Project(Name name, Version version, Libraries libraries, Spaces spaces) {

  public Project() {
    this(Name.of("noname"), Version.of("0"), Libraries.of(), Spaces.of());
  }

  public Project name(String name) {
    return new Project(Name.of(name), version, libraries, spaces);
  }

  public Project version(String version) {
    return version(Version.of(version));
  }

  public Project version(Version version) {
    return new Project(name, version, libraries, spaces);
  }

  public Project libraries(Libraries libraries) {
    return new Project(name, version, libraries, spaces);
  }
}
