package com.github.sormuras.bach;

import com.github.sormuras.bach.Options.Property;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;

public record Project(String name, Version version) {

  public static Project of(Bach bach) {
    var info = bach.computeProjectInfo();
    var options = bach.options();
    var name = info.name().equals("*") ? Path.of("").toAbsolutePath().getFileName().toString() : info.name();
    return new Project()
        .name(options.get(Property.PROJECT_NAME, name))
        .version(options.get(Property.PROJECT_VERSION, info.version()));
  }

  public Project() {
    this("noname", Version.parse("0"));
  }

  public Project name(String name) {
    return new Project(name, version);
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
