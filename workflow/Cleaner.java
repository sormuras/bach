/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public interface Cleaner extends Action {
  default void clean() {
    var folders = workflow().folders();
    delete(folders.out());
  }

  private void delete(Path file) {
    if (!Files.exists(file)) return;
    log("Delete " + file.toUri());
    try {
      try {
        Files.delete(file); // delete a regular file or an empty directory
      } catch (DirectoryNotEmptyException exception) {
        try (var stream = Files.walk(file)) {
          var paths = stream.sorted(Comparator.reverseOrder()).toList();
          for (var path : paths) Files.delete(path);
        }
      }
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}
