package de.sormuras.bach.task;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Log;
import de.sormuras.bach.Task;
import de.sormuras.bach.project.Folder;
import de.sormuras.bach.util.Paths;
import java.nio.file.Files;
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
      var jars = Paths.list(modules, "*.jar");
      log.info("%d jar(s) found in: %s", jars.size(), modules.toUri());
      for (var jar : jars) {
        log.info("%,11d %s", Files.size(jar), jar.getFileName());
      }
    }

    var duration = Duration.between(log.getInstant(), Instant.now());
    log.info("Build %d took millis.", duration.toMillis());

    writeSummaryLog(log, project.folder());
    writeSummaryMarkdown(log, project.folder());
  }

  private String toString(Log.Entry entry) {
    return String.format("%s|%s|%s", entry.instant(), entry.level(), entry.message());
  }

  private void writeSummaryLog(Log log, Folder folder) throws Exception {
    var entries = log.getEntries();
    var timestamp = log.getInstant().toString().replace(":", "-");
    var summary = folder.log("summary-" + timestamp + ".log");
    Files.createDirectories(folder.log());
    Files.write(summary, entries.stream().map(this::toString).collect(Collectors.toList()));
    Files.copy(summary, folder.out("summary.log"), StandardCopyOption.REPLACE_EXISTING);
  }

  private void writeSummaryMarkdown(Log log, Folder folder) throws Exception {
    var lines = new ArrayList<String>();
    lines.add("# Summary");
    lines.add("");
    lines.add("## Messages logged at INFO and more severe levels");
    lines.add("");
    for (var entry : log.getEntries()) {
      if (entry.level().getSeverity() >= System.Logger.Level.INFO.getSeverity()) {
        lines.add("- " + entry.message());
      }
    }
    lines.add("");
    lines.add("## All log entries");
    lines.add("");
    for (var entry : log.getEntries()) {
      lines.add("- " + entry.message());
    }
    lines.add("");
    Files.write(folder.out("summary.md"), lines);
  }
}
