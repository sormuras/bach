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

  static ModuleDeclaration of(Path path, boolean treatSourcesAsResources) {
    var info = Paths.isModuleInfoJavaFile(path) ? path : path.resolve("module-info.java");
    var reference = ModuleInfoReference.of(info);

    if (info.equals(Path.of("module-info.java")))
      return new ModuleDeclaration(reference, new SourceFolders(List.of()), new SourceFolders(List.of()));

    var parent = info.getParent();
    if (Paths.name(parent).equals(reference.name())) { // single source folder
      var folder = SourceFolder.of(parent);
      return new ModuleDeclaration(
          reference,
          new SourceFolders(List.of(folder)),
          new SourceFolders(treatSourcesAsResources ? List.of(folder) : List.of()));
    }
    // sources = "java", "java-module", or targeted "java.*?(\d+)$"
    // resources = "resources", or targeted "resource.*?(\d+)$"
    var space = parent.getParent(); // usually "main", "test", or equivalent
    if (space == null) throw new AssertionError("No parents' parent? info -> " + info);
    var sources = folders(space, "java");
    var resources = folders(space, treatSourcesAsResources ? "" : "resource");
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
