package com.github.sormuras.bach.project;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** A collection of possibly targeted folders. */
public record Folders(Collection<Folder> values) {

  public static Folders of(Folder... folders) {
    return new Folders(List.of(folders));
  }

  public Folders add(Folder folder) {
    var values = new ArrayList<>(this.values);
    values.add(folder);
    return new Folders(values);
  }

  public List<Path> list(int release, FolderType... types) {
    return list(release, List.of(types));
  }

  public List<Path> list(int release, List<FolderType> types) {
    return values.stream()
        .filter(path -> path.version() == release)
        .filter(path -> path.types().values().containsAll(types))
        .map(Folder::directory)
        .toList();
  }
}
