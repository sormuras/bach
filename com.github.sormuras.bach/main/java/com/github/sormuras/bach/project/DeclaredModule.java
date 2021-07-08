package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.ModuleDescriptors;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record DeclaredModule(ModuleDescriptor descriptor, Path info, DeclaredPaths paths)
    implements Comparable<DeclaredModule> {

  public static DeclaredModule of(String pathOfModuleInfoJavaFileOrItsParentDirectory) {
    var path = Path.of(pathOfModuleInfoJavaFileOrItsParentDirectory).normalize();
    if (Files.notExists(path)) throw new IllegalArgumentException("Path must exist: " + path);
    var info = Files.isDirectory(path) ? path.resolve("module-info.java") : path;
    if (Files.notExists(info)) throw new IllegalArgumentException("No module-info in: " + path);
    var descriptor = ModuleDescriptors.parse(info);
    var parent = info.getParent();
    var paths =
        List.of(
            new DeclaredPath(
                Set.of(PathType.SOURCES),
                parent != null ? parent : Path.of("."),
                Optional.empty()));
    return new DeclaredModule(descriptor, info, new DeclaredPaths(paths));
  }

  public String name() {
    return descriptor.name();
  }

  @Override
  public int compareTo(DeclaredModule other) {
    return name().compareTo(other.name());
  }

  public DeclaredModule withSources(String... paths) {
    return withSources(0, paths);
  }

  public DeclaredModule withSources(int release, String... paths) {
    return with(Set.of(PathType.SOURCES), release, paths);
  }

  public DeclaredModule withResources(String... paths) {
    return withResources(0, paths);
  }

  public DeclaredModule withResources(int release, String... paths) {
    return with(Set.of(PathType.RESOURCES), release, paths);
  }

  public DeclaredModule with(Set<PathType> types, int release, String... paths) {
    var list = new ArrayList<>(this.paths.list());
    for (var path : paths) {
      list.add(
          new DeclaredPath(
              Set.copyOf(types),
              Path.of(path).normalize(),
              Optional.ofNullable(release == 0 ? null : new JavaRelease(release))));
    }
    return new DeclaredModule(descriptor, info, new DeclaredPaths(list));
  }
}
