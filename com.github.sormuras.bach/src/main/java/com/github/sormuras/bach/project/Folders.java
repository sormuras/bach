package com.github.sormuras.bach.project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A list of source and resource folders, possibly targeted to a specific Java feature release
 * version.
 */
public record Folders(List<Folder> list) {

  public static Folders of(Path info) {
    var parent = info.getParent();
    if (parent == null) return new Folders(List.of());

    if (!parent.getFileName().toString().startsWith("java"))
      return new Folders(List.of(Folder.of(parent)));

    var grandpa = parent.getParent();
    if (grandpa == null) return new Folders(List.of(Folder.of(parent)));

    try (var stream = Files.list(grandpa)) {
      var folders = stream.filter(Folders::isSourcesOrResourcesDirectory).map(Folder::of);
      return new Folders(folders.sorted().toList());
    } catch (Exception exception) {
      throw new RuntimeException("Listing entries of %s failed".formatted(info));
    }
  }

  static boolean isSourcesOrResourcesDirectory(Path path) {
    if (!Files.isDirectory(path)) return false;
    var name = path.getFileName().toString();
    return name.startsWith("java") || name.startsWith("resources");
  }
}
