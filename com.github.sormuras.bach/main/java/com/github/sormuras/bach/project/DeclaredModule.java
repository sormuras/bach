package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.ModuleDescriptors;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;

public record DeclaredModule(ModuleDescriptor descriptor, Path path)
    implements Comparable<DeclaredModule> {

  public static DeclaredModule of(String string) {
    var path = Path.of(string).normalize();
    if (Files.notExists(path)) throw new IllegalArgumentException("Path must exist: " + path);
    var info = Files.isDirectory(path) ? path.resolve("module-info.java") : path;
    if (Files.notExists(info)) throw new IllegalArgumentException("No module-info in: " + path);
    return new DeclaredModule(ModuleDescriptors.parse(info), info);
  }

  public String name() {
    return descriptor.name();
  }

  @Override
  public int compareTo(DeclaredModule other) {
    return name().compareTo(other.name());
  }
}
