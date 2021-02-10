package com.github.sormuras.bach;

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

  public String versionNumberAndPreRelease() {
    return toNumberAndPreRelease(version);
  }

  public Project libraries(Libraries libraries) {
    return new Project(name, version, libraries);
  }

  /**
   * Returns a string containing the version number and, if present, the pre-release version.
   *
   * @param version the module's version
   * @return a string containing the version number and, if present, the pre-release version.
   */
  public static String toNumberAndPreRelease(Version version) {
    var string = version.toString();
    var firstPlus = string.indexOf('+');
    if (firstPlus == -1) return string;
    var secondPlus = string.indexOf('+', firstPlus + 1);
    return string.substring(0, secondPlus == -1 ? firstPlus : secondPlus);
  }
}
