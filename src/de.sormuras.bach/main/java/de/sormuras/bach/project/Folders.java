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
public final class Folders {

  private final List<Folder> list;

  public Folders(List<Folder> list) {
    this.list = List.copyOf(list);
  }

  public List<Folder> list() {
    return list;
  }

  //
  // Configuration API
  //

  static List<Folder> list(Path infoDirectory, int javaRelease) {
    var source = Folder.of(infoDirectory); // contains module-info.java file
    var parent = infoDirectory.getParent();
    if (javaRelease != 0 || source.release() == 0 || parent == null) {
      var java = infoDirectory.resolveSibling("java");
      if (java.equals(infoDirectory) || Files.notExists(java)) return List.of(source);
      return List.of(new Folder(java, javaRelease), source);
    }
    return listMapFilterSortedCollect(parent);
  }

  static List<Folder> listMapFilterSortedCollect(Path path) {
    return Paths.list(path, Files::isDirectory).stream()
        .map(Folder::of)
        .filter(Folder::isTargeted)
        .sorted(Comparator.comparingInt(Folder::release))
        .collect(Collectors.toUnmodifiableList());
  }

  @Factory
  public static Folders of() {
    return new Folders(List.of());
  }

  @Factory
  public static Folders of(Path infoDirectory) {
    return of(infoDirectory, 0);
  }

  @Factory
  public static Folders of(Path infoDirectory, int javaRelease) {
    return new Folders(list(infoDirectory, javaRelease));
  }

  @Factory(Kind.OPERATOR)
  public Folders with(Folder... additionalDirectories) {
    var directories = new ArrayList<>(list);
    directories.addAll(List.of(additionalDirectories));
    return new Folders(directories);
  }

  //
  // Normal API
  //

  public Folder first() {
    return list.get(0);
  }

  public Folder last() {
    return list.get(list.size() - 1);
  }

  public boolean isMultiTarget() {
    if (list.isEmpty()) return false;
    if (list.size() == 1) return first().isTargeted();
    return list.stream().allMatch(Folder::isTargeted);
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
