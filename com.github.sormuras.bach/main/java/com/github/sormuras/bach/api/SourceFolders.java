package com.github.sormuras.bach.api;

import com.github.sormuras.bach.internal.Strings;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public record SourceFolders(List<SourceFolder> list) {

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
    if (list.isEmpty()) return List.of(Path.of("."));
    var first = first();
    if (first.isModuleInfoJavaPresent()) return List.of(first.path());
    for (var folder : list)
      if (folder.isModuleInfoJavaPresent()) return List.of(first.path(), folder.path());
    throw new IllegalStateException("No module-info.java found in: " + list);
  }
}
