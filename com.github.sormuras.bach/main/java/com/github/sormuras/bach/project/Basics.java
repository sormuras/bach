package com.github.sormuras.bach.project;

import java.lang.module.ModuleDescriptor;

public record Basics(String name, ModuleDescriptor.Version version) {

  public static Basics of(String name, String version) {
    return new Basics(name, ModuleDescriptor.Version.parse(version));
  }

  public Basics name(String name) {
    return new Basics(name, version);
  }

  public Basics version(String version) {
    return version(ModuleDescriptor.Version.parse(version));
  }

  public Basics version(ModuleDescriptor.Version version) {
    return new Basics(name, version);
  }
}
