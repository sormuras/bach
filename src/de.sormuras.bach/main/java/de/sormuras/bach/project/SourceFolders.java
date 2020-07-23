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

import de.sormuras.bach.internal.Factory;
import de.sormuras.bach.internal.Factory.Kind;
import de.sormuras.bach.internal.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** A list of source folder objects. */
public final class SourceFolders {

  private final List<SourceFolder> list;

  public SourceFolders(List<SourceFolder> list) {
    this.list = List.copyOf(list);
  }

  public List<SourceFolder> list() {
    return list;
  }

  //
  // Configuration API
  //

  static List<SourceFolder> list(Path infoDirectory, int javaRelease) {
    var source = SourceFolder.of(infoDirectory); // contains module-info.java file
    var parent = infoDirectory.getParent();
    if (javaRelease != 0 || source.release() == 0 || parent == null) {
      var java = infoDirectory.resolveSibling("java");
      if (java.equals(infoDirectory) || Files.notExists(java)) return List.of(source);
      return List.of(new SourceFolder(java, javaRelease), source);
    }
    return listMapFilterSortedCollect(parent);
  }

  static List<SourceFolder> listMapFilterSortedCollect(Path path) {
    return Paths.list(path, Files::isDirectory).stream()
        .map(SourceFolder::of)
        .filter(SourceFolder::isTargeted)
        .sorted(Comparator.comparingInt(SourceFolder::release))
        .collect(Collectors.toUnmodifiableList());
  }

  @Factory
  public static SourceFolders of() {
    return new SourceFolders(List.of());
  }

  @Factory
  public static SourceFolders of(Path infoDirectory) {
    return of(infoDirectory, 0);
  }

  @Factory
  public static SourceFolders of(Path infoDirectory, int javaRelease) {
    return new SourceFolders(list(infoDirectory, javaRelease));
  }

  @Factory(Kind.OPERATOR)
  public SourceFolders with(SourceFolder... additionalDirectories) {
    var directories = new ArrayList<>(list);
    directories.addAll(List.of(additionalDirectories));
    return new SourceFolders(directories);
  }

  //
  // Normal API
  //

  public SourceFolder first() {
    return list.get(0);
  }

  public SourceFolder last() {
    return list.get(list.size() - 1);
  }

  public boolean isMultiTarget() {
    if (list.isEmpty()) return false;
    if (list.size() == 1) return first().isTargeted();
    return list.stream().allMatch(SourceFolder::isTargeted);
  }

  public String toModuleSpecificSourcePath() {
    return Paths.join(toModuleSpecificSourcePaths());
  }

  public List<Path> toModuleSpecificSourcePaths() {
    var first = first();
    if (first.isModuleInfoJavaPresent()) return List.of(first.path());
    for (var directory : list)
      if (directory.isModuleInfoJavaPresent()) return List.of(first.path(), directory.path());
    throw new IllegalStateException("No module-info.java found in: " + list);
  }
}
