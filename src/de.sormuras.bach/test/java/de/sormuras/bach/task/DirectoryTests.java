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

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.API;
import de.sormuras.bach.Bach;
import de.sormuras.bach.Task;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.base.Log;
import test.base.Tree;

class DirectoryTests {

  @Test
  void createAndDeleteAnEmptyDirectory(@TempDir Path temp) {
    var log = new Log();
    var bach = new Bach(log, true, false);
    var empty = temp.resolve("empty");
    bach.execute(new CreateDirectories(empty));
    assertTrue(Files.isDirectory(empty));
    bach.execute(new DeleteDirectories(empty));
    assertTrue(Files.notExists(empty));
    log.assertThatEverythingIsFine();
  }

  @Test
  void createAndFillAndDeleteRootDirectory(@TempDir Path temp) {

    class FillDirectory extends Task {
      final Path directory;
      final int count;

      FillDirectory(Path directory, int count) {
        super("FillDirectory");
        this.directory = directory;
        this.count = count;
      }

      @Override
      public void execute(Execution execution) throws Exception {
        for (int i = 1; i <= count; i++) Files.createFile(directory.resolve("file" + i));
      }
    }

    var log = new Log();
    var bach = new Bach(log, true, false);
    var root = temp.resolve("root");
    var nest = root.resolve("sub/nest");
    var task =
        Task.sequence(
            "Create, fill, and delete root directory",
            new CreateDirectories(nest),
            Task.parallel(
                "Fill directories",
                new FillDirectory(root, 1),
                new FillDirectory(nest.getParent(), 2),
                new FillDirectory(nest, 3)),
            API.taskOf(
                "Check tree",
                __ ->
                    assertLinesMatch(
                        List.of(
                            "file1",
                            "sub",
                            "sub/file1",
                            "sub/file2",
                            "sub/nest",
                            "sub/nest/file1",
                            "sub/nest/file2",
                            "sub/nest/file3"),
                        Tree.walk(root))),
            new DeleteDirectories(root));
    bach.execute(task);
    assertTrue(Files.notExists(root));
    log.assertThatEverythingIsFine();
  }
}
