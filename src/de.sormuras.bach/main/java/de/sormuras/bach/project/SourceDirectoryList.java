/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach.project;

import de.sormuras.bach.internal.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** A non-empty list of source directory objects. */
public final class SourceDirectoryList {

  public static SourceDirectoryList of(Path infoDirectory) {
    return new SourceDirectoryList(list(infoDirectory));
  }

  static List<SourceDirectory> list(Path infoDirectory) {
    var source = SourceDirectory.of(infoDirectory); // contains module-info.java file
    var parent = infoDirectory.getParent();
    if (source.release() == 0 || parent == null) {
      var java = infoDirectory.resolveSibling("java");
      if (java.equals(infoDirectory) || Files.notExists(java)) return List.of(source);
      return List.of(new SourceDirectory(java, 0), source);
    }
    return Paths.list(parent, Files::isDirectory).stream()
        .map(SourceDirectory::of)
        .filter(SourceDirectory::isTargeted)
        .sorted(Comparator.comparingInt(SourceDirectory::release))
        .collect(Collectors.toUnmodifiableList());
  }

  private final List<SourceDirectory> directories;

  public SourceDirectoryList(List<SourceDirectory> directories) {
    this.directories = List.copyOf(directories);
  }

  public List<SourceDirectory> directories() {
    return directories;
  }

  public SourceDirectory first() {
    return directories.get(0);
  }

  public SourceDirectory last() {
    return directories.get(directories.size() - 1);
  }

  public boolean isMultiTarget() {
    if (directories.isEmpty()) return false;
    if (directories.size() == 1) return first().isTargeted();
    return directories.stream().allMatch(SourceDirectory::isTargeted);
  }

  public String toModuleSpecificSourcePath() {
    return Paths.join(toModuleSpecificSourcePaths());
  }

  public List<Path> toModuleSpecificSourcePaths() {
    var first = first();
    if (first.isModuleInfoJavaPresent()) return List.of(first.path());
    for (var directory : directories)
      if (directory.isModuleInfoJavaPresent()) return List.of(first.path(), directory.path());
    throw new IllegalStateException("No module-info.java found in: " + directories);
  }
}
