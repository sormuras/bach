package com.github.sormuras.bach;

public abstract class Workflow {

  protected final Bach bach;
  protected final Project project;

  protected Workflow(Bach bach) {
    this.bach = bach;
    this.project = bach.project();
  }

  public final Bach bach() {
    return bach;
  }

  public abstract void execute();

  protected void checkpoint(Checkpoint checkpoint) {
    bach.settings().workflowSettings().checkpointHandler().handle(checkpoint);
  }
}
