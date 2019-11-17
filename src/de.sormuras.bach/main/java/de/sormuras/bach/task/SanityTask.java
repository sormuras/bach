package de.sormuras.bach.task;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Log;
import de.sormuras.bach.Task;
import java.nio.file.Files;

public class SanityTask implements Task {

  @Override
  public void execute(Bach bach) throws Exception {
    var log = bach.getLog();
    var project = bach.getProject();
    var realms = project.structure().realms();
    if (realms.isEmpty()) throw error(log, "No realm configured in project");
    var base = project.folder().base();
    if (!Files.isDirectory(base)) {
      throw error(log, "Base must be a directory: %s", base.toUri());
    }
    try (var stream = Files.newDirectoryStream(base)) {
      if (!stream.iterator().hasNext()) {
        throw error(log, "Base directory is empty: %s", base.toUri());
      }
    }
  }

  private static Error error(Log log, String format, Object... args) {
    return new AssertionError(log.warning(format, args).message());
  }
}
