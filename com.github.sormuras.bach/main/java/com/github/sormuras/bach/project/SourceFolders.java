package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.Strings;
import java.nio.file.Path;
import java.util.List;

/** A list of source folder objects. */
public record SourceFolders(List<SourceFolder> list) {

  /** @return the first source folder in the underlying list */
  public SourceFolder first() {
    return list.get(0);
  }

  /** @return a string for {@code javac --module-source-path PATH} in module-specific form */
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