package de.sormuras.bach.task;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Log;
import de.sormuras.bach.Task;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class SummaryTask implements Task {

  @Override
  public void execute(Bach bach) throws Exception {
    var log = bach.getLog();
    var project = bach.getProject();
    var realms = project.structure().realms();
    if (realms.isEmpty()) {
      log.warning("No realm configured in project: %s", project);
    }
    for (var realm : realms) {
      log.info("Modules of %s realm", realm.name());
      var modules = project.folder().modules(realm.name());
      if (Files.notExists(modules)) {
        if (!project.units(realm).isEmpty()) {
          log.warning("Modules folder not found: %s", modules);
        }
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

    var duration = Duration.between(log.getInstant(), Instant.now());
    log.info("Build %d took millis.", duration.toMillis());

    var entries = log.getEntries();
    var timestamp = log.getInstant().toString().replace(":", "-");
    var summary = project.folder().log("summary-" + timestamp + ".log");
    Files.createDirectories(project.folder().log());
    Files.write(summary, entries.stream().map(this::toString).collect(Collectors.toList()));
    Files.copy(summary, project.folder().out("summary.log"), StandardCopyOption.REPLACE_EXISTING);
  }

  private String toString(Log.Entry entry) {
    return String.format("%s|%s|%s", entry.instant(), entry.level(), entry.message());
  }
}
