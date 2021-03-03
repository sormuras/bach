package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.internal.Strings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/** A list of source folder objects. */
public record SourceFolders(List<SourceFolder> list) {

  public static SourceFolders of(Path path, String prefix) {
    var paths =
        Paths.list(path, Files::isDirectory).stream()
            .filter(candidate -> Strings.name(candidate).startsWith(prefix))
            .map(SourceFolder::of)
            .sorted(Comparator.comparingInt(SourceFolder::release))
            .toList();
    return new SourceFolders(paths);
  }

  /** @return the first source folder in the underlying list */
  public SourceFolder first() {
    return list.get(0);
  }

  /** @return a string for {@code javac --module-source-path PATH} in module-specific form */
  public String toModuleSpecificSourcePath() {
    return Strings.join(toModuleSpecificSourcePaths());
  }

  List<Path> toModuleSpecificSourcePaths() {
    var first = list.isEmpty() ? SourceFolder.of(Path.of(".")) : first();
    if (first.isModuleInfoJavaPresent()) return List.of(first.path());
    for (var folder : list)
      if (folder.isModuleInfoJavaPresent()) return List.of(first.path(), folder.path());
    throw new IllegalStateException("No module-info.java found in: " + list);
  }
}