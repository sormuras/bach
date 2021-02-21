package com.github.sormuras.bach.project;

import java.lang.module.ModuleDescriptor;

public record Version(ModuleDescriptor.Version value) {

  public static Version of(String version) {
    return new Version(ModuleDescriptor.Version.parse(version));
  }

  public String toNumberAndPreRelease() {
    var string = value.toString();
    var firstPlus = string.indexOf('+');
    if (firstPlus == -1) return string;
    var secondPlus = string.indexOf('+', firstPlus + 1);
    return string.substring(0, secondPlus == -1 ? firstPlus : secondPlus);
  }
}
