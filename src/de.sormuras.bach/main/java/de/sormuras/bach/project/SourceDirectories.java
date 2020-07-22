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

/** A list of source directory objects. */
public final class SourceDirectories {

  private final List<SourceDirectory> list;

  public SourceDirectories(List<SourceDirectory> list) {
    this.list = List.copyOf(list);
  }

  public List<SourceDirectory> list() {
    return list;
  }

  //
  // Configuration API
  //

  static List<SourceDirectory> list(Path infoDirectory, int javaRelease) {
    var source = SourceDirectory.of(infoDirectory); // contains module-info.java file
    var parent = infoDirectory.getParent();
    if (javaRelease != 0 || source.release() == 0 || parent == null) {
      var java = infoDirectory.resolveSibling("java");
      if (java.equals(infoDirectory) || Files.notExists(java)) return List.of(source);
      return List.of(new SourceDirectory(java, javaRelease), source);
    }
    return listMapFilterSortedCollect(parent);
  }

  static List<SourceDirectory> listMapFilterSortedCollect(Path path) {
    return Paths.list(path, Files::isDirectory).stream()
        .map(SourceDirectory::of)
        .filter(SourceDirectory::isTargeted)
        .sorted(Comparator.comparingInt(SourceDirectory::release))
        .collect(Collectors.toUnmodifiableList());
  }

  @Factory
  public static SourceDirectories of() {
    return new SourceDirectories(List.of());
  }

  @Factory
  public static SourceDirectories of(Path infoDirectory) {
    return of(infoDirectory, 0);
  }

  @Factory
  public static SourceDirectories of(Path infoDirectory, int javaRelease) {
    return new SourceDirectories(list(infoDirectory, javaRelease));
  }

  @Factory(Kind.OPERATOR)
  public SourceDirectories with(SourceDirectory... additionalDirectories) {
    var directories = new ArrayList<>(list);
    directories.addAll(List.of(additionalDirectories));
    return new SourceDirectories(directories);
  }

  //
  // Normal API
  //

  public SourceDirectory first() {
    return list.get(0);
  }

  public SourceDirectory last() {
    return list.get(list.size() - 1);
  }

  public boolean isMultiTarget() {
    if (list.isEmpty()) return false;
    if (list.size() == 1) return first().isTargeted();
    return list.stream().allMatch(SourceDirectory::isTargeted);
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
