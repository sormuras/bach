package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.ModuleDescriptors;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public record Module(ModuleDescriptor descriptor, URI location) implements Comparable<Module> {

  public static Module of(String string) {
    var path = Path.of(string).normalize();
    if (Files.notExists(path)) throw new IllegalArgumentException("Path must exist: " + path);
    var info = Files.isDirectory(path) ? path.resolve("module-info.java") : path;
    if (Files.notExists(info)) throw new IllegalArgumentException("No module-info in: " + path);
    return new Module(ModuleDescriptors.parse(info), info.toUri());
  }

  public String name() {
    return descriptor.name();
  }

  @Override
  public int compareTo(Module other) {
    return name().compareTo(other.name());
  }
}
