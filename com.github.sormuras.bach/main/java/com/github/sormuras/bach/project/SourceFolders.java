package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.Strings;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** A list of source folder objects. */
public record SourceFolders(List<SourceFolder> list) {

  /** {@return the first source folder in the underlying list} */
  public SourceFolder first() {
    return list.get(0);
  }

  /** {@return the first source folder that targets the specified release} */
  public Optional<SourceFolder> targets(int release) {
    for (var folder : list) if (folder.release() == release) return Optional.of(folder);
    return Optional.empty();
  }

  /** {@return a string for {@code javac --module-source-path PATH} in module-specific form} */
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
