package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.ModuleDescriptorSupport;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;

public record DeclaredModule(Path info, ModuleDescriptor descriptor, Folders folders)
    implements Comparable<DeclaredModule> {
  public static DeclaredModule of(Path path) {
    var info = path.endsWith("module-info.java") ? path : path.resolve("module-info.java");
    var descriptor = ModuleDescriptorSupport.parse(info.normalize());
    var folders = Folders.of(info);
    return new DeclaredModule(info, descriptor, folders);
  }

  public String name() {
    return descriptor.name();
  }

  public List<Path> toModuleSourcePaths() {
    var parent = info.getParent();
    return parent != null ? List.of(parent) : List.of(Path.of("."));
  }

  @Override
  public int compareTo(DeclaredModule other) {
    return name().compareTo(other.name());
  }
}
