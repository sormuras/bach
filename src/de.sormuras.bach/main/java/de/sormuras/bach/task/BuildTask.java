package de.sormuras.bach.task;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Task;

public class BuildTask implements Task {

  @Override
  public void execute(Bach bach) throws InterruptedException {
    bach.execute(new SanityTask());
    Thread.sleep(123); // Here be dragons!
    bach.execute(new SummaryTask());
  }
}
