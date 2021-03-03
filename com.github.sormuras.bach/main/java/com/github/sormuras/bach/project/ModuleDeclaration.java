package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.Strings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A module declaration connects a module compilation unit with source folders and resource folders.
 *
 * @param reference the module info reference
 * @param sources the source folders
 * @param resources the resource folders
 */
public record ModuleDeclaration(
    ModuleInfoReference reference, SourceFolders sources, SourceFolders resources)
    implements Comparable<ModuleDeclaration> {

  @Override
  public int compareTo(ModuleDeclaration other) {
    return name().compareTo(other.name());
  }

  /** {@return the module name} */
  public String name() {
    return reference.name();
  }

  public static ModuleDeclaration of(Path path, boolean treatSourcesAsResources) {
    return of(Path.of(""), path, treatSourcesAsResources);
  }

  public static ModuleDeclaration of(Path root, Path path, boolean treatSourcesAsResources) {
    var info = Files.isDirectory(path) ? path.resolve("module-info.java") : path;
    var reference = ModuleInfoReference.of(info);

    // no source folder, module declaration in root directory
    // "module-info.java"
    if (root.relativize(info).getNameCount() == 1)
      return new ModuleDeclaration(
          reference, new SourceFolders(List.of()), new SourceFolders(List.of()));

    // assume single source folder, directory and module names are equal
    // "foo.bar/module-info.java" with "module foo.bar {...}"
    var parent = info.getParent();
    if (Strings.name(parent).equals(reference.name())) {
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
    var sources = SourceFolders.of(space, "java");
    var resources = SourceFolders.of(space, treatSourcesAsResources ? "" : "resource");
    return new ModuleDeclaration(reference, sources, resources);
  }
}
