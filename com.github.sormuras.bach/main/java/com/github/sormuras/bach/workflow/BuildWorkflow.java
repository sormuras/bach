package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Workflow;
import com.github.sormuras.bach.internal.Durations;
import java.lang.System.Logger.Level;
import java.time.Instant;

public class BuildWorkflow extends Workflow {

  public BuildWorkflow(Bach bach) {
    super(bach);
  }

  @Override
  public void execute() {
    bach.log(Level.INFO, "Build project %s", project.toNameAndVersion());
    var start = Instant.now();
    try {
      checkpoint(new BeginCheckpoint(bach));
      bach.manageExternalModules();
      bach.compileMainSpace();
      bach.compileTestSpace();
      bach.executeTests();
      checkpoint(new EndCheckpoint(bach));
    } catch (Exception exception) {
      bach.logbook().log(exception);
      checkpoint(new ErrorCheckpoint(bach, exception));
      throw new RuntimeException("Build failed!", exception);
    } finally {
      bach.writeLogbook();
      bach.log(
          Level.INFO,
          "Build of project %s took %s",
          project.toNameAndVersion(),
          Durations.beautifyBetweenNow(start));
      checkpoint(new FinalCheckpoint(bach));
    }
  }

  public record BeginCheckpoint(Bach bach) implements Checkpoint {}

  public record EndCheckpoint(Bach bach) implements Checkpoint {}

  public record ErrorCheckpoint(Bach bach, Exception exception) implements Checkpoint {}

  public record FinalCheckpoint(Bach bach) implements Checkpoint {}
}
