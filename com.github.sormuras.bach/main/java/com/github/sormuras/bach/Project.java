package com.github.sormuras.bach;

import com.github.sormuras.bach.project.Basics;
import com.github.sormuras.bach.project.Libraries;
import com.github.sormuras.bach.project.Spaces;

public record Project(Basics info, Libraries libraries, Spaces spaces) {

  public Project() {
    this(Basics.of("noname", "0"), Libraries.of(), Spaces.of());
  }

  public Project name(String name) {
    return new Project(info.name(name), libraries, spaces);
  }

  public Project version(String version) {
    return new Project(info.version(version), libraries, spaces);
  }

  public Project libraries(Libraries libraries) {
    return new Project(info, libraries, spaces);
  }

  public String name() {
    return info.name();
  }

  public String version() {
    return info.version().toString();
  }

  public String versionNumberAndPreRelease() {
    var string = version();
    var firstPlus = string.indexOf('+');
    if (firstPlus == -1) return string;
    var secondPlus = string.indexOf('+', firstPlus + 1);
    return string.substring(0, secondPlus == -1 ? firstPlus : secondPlus);
  }
}
