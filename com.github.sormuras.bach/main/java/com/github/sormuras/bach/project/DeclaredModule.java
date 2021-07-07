package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.ModuleDescriptors;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public record DeclaredModule(ModuleDescriptor descriptor, Path info, List<Path> paths)
    implements Comparable<DeclaredModule> {

  public static DeclaredModule of(String location, String... additionalSourcePaths) {
    var path = Path.of(location).normalize();
    if (Files.notExists(path)) throw new IllegalArgumentException("Path must exist: " + path);
    var info = Files.isDirectory(path) ? path.resolve("module-info.java") : path;
    if (Files.notExists(info)) throw new IllegalArgumentException("No module-info in: " + path);
    var descriptor = ModuleDescriptors.parse(info);
    var parent = info.getParent();
    var paths =
        Stream.concat(
                Stream.of(parent != null ? parent : Path.of(".")),
                Stream.of(additionalSourcePaths).map(Path::of).map(Path::normalize))
            .distinct()
            .toList();
    return new DeclaredModule(descriptor, info, paths);
  }

  public String name() {
    return descriptor.name();
  }

  @Override
  public int compareTo(DeclaredModule other) {
    return name().compareTo(other.name());
  }
}
