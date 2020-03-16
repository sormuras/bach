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

package de.sormuras.bach.execution.task;

import de.sormuras.bach.execution.ExecutionContext;
import de.sormuras.bach.execution.ExecutionResult;
import de.sormuras.bach.execution.Task;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Delete a tree of directories. */
public /*static*/ class DeleteDirectories extends Task {

  private final Path path;

  public DeleteDirectories(Path path) {
    super("Delete directories " + path, false, List.of());
    this.path = path;
  }

  @Override
  public ExecutionResult execute(ExecutionContext context) {
    try {
      delete(path, __ -> true);
      return context.ok();
    } catch (Exception e) {
      return context.failed(e);
    }
  }

  static void delete(Path directory, Predicate<Path> filter) throws Exception {
    // trivial case: delete existing empty directory or single file
    try {
      Files.deleteIfExists(directory);
      return;
    } catch (DirectoryNotEmptyException ignored) {
      // fall-through
    }
    // default case: walk the tree...
    try (var stream = Files.walk(directory)) {
      var paths = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
      for (var path : paths.collect(Collectors.toList())) {
        Files.deleteIfExists(path);
      }
    }
  }
}
