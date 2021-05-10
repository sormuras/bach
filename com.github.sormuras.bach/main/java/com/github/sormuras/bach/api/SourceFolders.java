package com.github.sormuras.bach.api;

import com.github.sormuras.bach.internal.Strings;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public record SourceFolders(List<SourceFolder> list) {

  public static SourceFolders of(SourceFolder... folders) {
    return new SourceFolders(List.of(folders));
  }

  public SourceFolder first() {
    if (list.isEmpty()) throw new IllegalStateException("No source folder in list");
    return list.get(0);
  }

  public Stream<SourceFolder> stream(int release) {
    return list.stream().filter(sourceFolder -> sourceFolder.release() == release);
  }

  public String toModuleSpecificSourcePath() {
    return Strings.join(toModuleSpecificSourcePaths());
  }

  List<Path> toModuleSpecificSourcePaths() {
    var first = first();
    var path = first.path();
    if (first.isModuleInfoJavaPresent()) return List.of(path);
    for (var next : list) if (next.isModuleInfoJavaPresent()) return List.of(path, next.path());
    throw new IllegalStateException("No module-info.java found in: " + list);
  }
}
