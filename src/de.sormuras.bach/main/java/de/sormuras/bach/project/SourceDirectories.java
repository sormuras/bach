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

import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

/** A set of source directory objects. */
public final class SourceDirectories {

  public static SourceDirectories of() {
    return new SourceDirectories(Set.of());
  }

  public static SourceDirectories of(Path infoDirectory) {
    return new SourceDirectories(SourceDirectory.ofAll(infoDirectory));
  }

  public SourceDirectories with(SourceDirectory directory) {
    var directories = toTreeSet();
    directories.add(directory);
    return new SourceDirectories(directories);
  }

  private final Set<SourceDirectory> directories;

  public SourceDirectories(Set<SourceDirectory> directories) {
    this.directories = Set.copyOf(directories);
  }

  public Set<SourceDirectory> directories() {
    return directories;
  }

  public boolean isMultiTarget() {
    if (directories.isEmpty()) return false;
    if (directories.size() == 1) return directories.iterator().next().isTargeted();
    return directories.stream().allMatch(SourceDirectory::isTargeted);
  }

  public TreeSet<SourceDirectory> toTreeSet() {
    return new TreeSet<>(directories);
  }
}
