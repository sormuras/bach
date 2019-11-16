package de.sormuras.bach.task;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Task;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public class SummaryTask implements Task {

  @Override
  public void execute(Bach bach) throws Exception {
    var log = bach.getLog();
    var project = bach.getProject();

    for (var realm : project.structure().realms()) {
      log.info("Modules of %s realm", realm.name());
      var modules = project.folder().modules(realm.name());
      if (Files.notExists(modules)) {
        log.info("Modules folder not found: " + modules);
        continue;
      }
      var jars = new ArrayList<Path>();
      try (var stream = Files.newDirectoryStream(modules, "*.jar")) {
        stream.forEach(jars::add);
      }
      log.info("%d jar(s) found in: %s", jars.size(), modules.toUri());
      for (var jar : jars) {
        log.info("%,11d %s", Files.size(jar), jar.getFileName());
      }
    }

    var duration = Duration.between(log.created(), Instant.now());
    log.info("Build %d took millis.", duration.toMillis());
  }
}
