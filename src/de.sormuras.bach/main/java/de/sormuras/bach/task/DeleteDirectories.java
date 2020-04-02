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

package de.sormuras.bach.task;

import de.sormuras.bach.Task;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

/** Delete a tree of directories. */
public /*static*/ class DeleteDirectories extends Task {

  private final Path path;

  public DeleteDirectories(Path path) {
    super("Delete directories " + path);
    this.path = path;
  }

  @Override
  public void execute(Execution context) throws Exception {
    delete(path, __ -> true);
  }

  static void delete(Path directory, Predicate<Path> filter) throws Exception {
    // trivial case: delete existing empty directory or single file
    try {
      Files.deleteIfExists(directory);
      return;
    } catch (DirectoryNotEmptyException __) {
      // fall-through
    }
    // default case: walk the tree from leaves back to root directories...
    try (var stream = Files.walk(directory)) {
      var paths = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
      for (var path : paths.toArray(Path[]::new)) Files.deleteIfExists(path);
    }
  }
}
