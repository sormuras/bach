package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A module declaration connects a module compilation unit with source folders and resource paths.
 *
 * @param reference the module info reference
 * @param sources the source folders
 * @param resources the resource paths
 */
public record ModuleDeclaration(
    ModuleInfoReference reference,
    SourceFolders sources,
    List<Path> resources)
    implements Comparable<ModuleDeclaration> {

  @Override
  public int compareTo(ModuleDeclaration other) {
    return name().compareTo(other.name());
  }

  /** @return the name of the module descriptor component */
  public String name() {
    return reference.descriptor().name();
  }

  static ModuleDeclaration of(Path path, int defaultJavaRelease) {
    var info = Paths.isModuleInfoJavaFile(path) ? path : path.resolve("module-info.java");
    var reference = ModuleInfoReference.of(info);
    var parent = info.getParent() != null ? info.getParent() : Path.of(".");
    var directories = SourceFolders.of(parent, defaultJavaRelease);
    var resources = resources(parent);
    return new ModuleDeclaration(reference, directories, resources);
  }

  static List<Path> resources(Path infoDirectory) {
    var resources = infoDirectory.resolveSibling("resources");
    return Files.isDirectory(resources) ? List.of(resources) : List.of();
  }
}
