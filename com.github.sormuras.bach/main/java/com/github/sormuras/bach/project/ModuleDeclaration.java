package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A module declaration connects a module compilation unit with source folders and resource paths.
 *
 * @param reference the module info reference
 * @param sources the source folders
 * @param resources the resource paths
 */
public record ModuleDeclaration(
    ModuleInfoReference reference, SourceFolders sources, SourceFolders resources)
    implements Comparable<ModuleDeclaration> {

  @Override
  public int compareTo(ModuleDeclaration other) {
    return name().compareTo(other.name());
  }

  /** @return the module name */
  public String name() {
    return reference.name();
  }

  /** @return {@code true} of source and resources lists are emtpy */
  public boolean simplicisissums() {
    return sources.list().isEmpty() && resources.list().isEmpty();
  }

  static ModuleDeclaration of(Path path, boolean resourcesIncludeSources) {
    var info = Paths.isModuleInfoJavaFile(path) ? path : path.resolve("module-info.java");
    var reference = ModuleInfoReference.of(info);
    var parent = info.getParent();
    if (parent == null) // simplicissimus-style
    return new ModuleDeclaration(
          reference,
          new SourceFolders(List.of()), // no source folders
          new SourceFolders(List.of()) // no resource folder
          );
    if (Paths.name(parent).equals(reference.name())) { // single source folder
      var folder = SourceFolder.of(parent);
      return new ModuleDeclaration(
          reference,
          new SourceFolders(List.of(folder)),
          new SourceFolders(resourcesIncludeSources ? List.of(folder) : List.of()));
    }
    // sources = "java", "java-module", or targeted "java.*?(\d+)$"
    // resources = "resources", or targeted "resource.*?(\d+)$"
    var space = parent.getParent(); // usually "main", "test", or equivalent
    if (space == null) throw new AssertionError("No parents' parent? info -> " + info);
    var sources = folders(space, "java");
    var resources = folders(space, resourcesIncludeSources ? "" : "resource");
    return new ModuleDeclaration(reference, sources, resources);
  }

  static SourceFolders folders(Path path, String prefix) {
    var paths =
        Paths.list(path, Files::isDirectory).stream()
            .filter(candidate -> Paths.name(candidate).startsWith(prefix))
            .map(SourceFolder::of)
            .sorted(Comparator.comparingInt(SourceFolder::release))
            .collect(Collectors.toUnmodifiableList());

    return new SourceFolders(paths);
  }
}
