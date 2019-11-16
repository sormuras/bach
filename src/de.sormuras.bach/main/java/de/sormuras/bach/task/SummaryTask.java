package de.sormuras.bach.task;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Task;
import java.time.Duration;
import java.time.Instant;

public class SummaryTask implements Task {

  @Override
  public void execute(Bach bach) {
    var log = bach.log();
    var duration = Duration.between(log.created(), Instant.now());
    log.info("Build %d took millis.", duration.toMillis());
  }
}
