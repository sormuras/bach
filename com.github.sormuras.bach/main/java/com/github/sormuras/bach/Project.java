package com.github.sormuras.bach;

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;

public record Project(String name, Version version) {

  public Project() {
    this(defaultName("noname"), defaultVersion("0"));
  }

  public Project version(String version) {
    return version(Version.parse(version));
  }

  public Project version(Version version) {
    return new Project(name, version);
  }

  public String versionNumberAndPreRelease() {
    return toNumberAndPreRelease(version);
  }

  public static String defaultName(String defaultName) {
    var name = Path.of("").toAbsolutePath().getFileName();
    return System.getProperty("bach.project.name", name != null ? name.toString() : defaultName);
  }

  public static Version defaultVersion(String defaultVersion) {
    return Version.parse(System.getProperty("bach.project.version", defaultVersion));
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
