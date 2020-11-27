package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.module.ModuleInfoReference;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A module declaration connects a module descriptor with source folders and resource paths.
 *
 * @param descriptor the module descriptor
 * @param sources the source folders
 * @param resources the resource paths
 */
public record ModuleDeclaration(
    ModuleDescriptor descriptor, SourceFolders sources, List<Path> resources)
    implements Comparable<ModuleDeclaration> {

  @Override
  public int compareTo(ModuleDeclaration other) {
    return name().compareTo(other.name());
  }

  /** @return the name of the module descriptor component */
  public String name() {
    return descriptor().name();
  }

  static ModuleDeclaration of(Path path) {
    return of(path, 0);
  }

  static ModuleDeclaration of(Path path, int defaultJavaRelease) {
    var info = Paths.isModuleInfoJavaFile(path) ? path : path.resolve("module-info.java");
    var descriptor = ModuleInfoReference.of(info).descriptor();
    var parent = info.getParent() != null ? info.getParent() : Path.of(".");
    var directories = SourceFolders.of(parent, defaultJavaRelease);
    var resources = resources(parent);
    return new ModuleDeclaration(descriptor, directories, resources);
  }

  static List<Path> resources(Path infoDirectory) {
    var resources = infoDirectory.resolveSibling("resources");
    return Files.isDirectory(resources) ? List.of(resources) : List.of();
  }
}
