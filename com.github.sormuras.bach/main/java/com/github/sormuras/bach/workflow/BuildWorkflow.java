package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Checkpoint;
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
      checkpoint(new StartCheckpoint(this));
      bach.manageExternalModules();
      bach.compileMainSpace();
      bach.compileTestSpace();
      bach.executeTests();
      checkpoint(new SuccessCheckpoint(this));
    } catch (Exception exception) {
      bach.logbook().log(exception);
      checkpoint(new ErrorCheckpoint(this, exception));
      throw new RuntimeException("Build failed!", exception);
    } finally {
      bach.writeLogbook();
      bach.log(
          Level.INFO,
          "Build of project %s took %s",
          project.toNameAndVersion(),
          Durations.beautifyBetweenNow(start));
      checkpoint(new FinalCheckpoint(this));
    }
  }

  public record StartCheckpoint(Workflow workflow) implements Checkpoint {}

  public record SuccessCheckpoint(Workflow workflow) implements Checkpoint {}

  public record ErrorCheckpoint(Workflow workflow, Exception exception) implements Checkpoint {}

  public record FinalCheckpoint(Workflow workflow) implements Checkpoint {}
}
