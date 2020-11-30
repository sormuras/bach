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

package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.Paths;
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
    return Paths.join(toModuleSpecificSourcePaths());
  }

  List<Path> toModuleSpecificSourcePaths() {
    var first = list.isEmpty() ? SourceFolder.of(Path.of(".")) : first();
    if (first.isModuleInfoJavaPresent()) return List.of(first.path());
    for (var directory : list)
      if (directory.isModuleInfoJavaPresent()) return List.of(first.path(), directory.path());
    throw new IllegalStateException("No module-info.java found in: " + list);
  }
}
