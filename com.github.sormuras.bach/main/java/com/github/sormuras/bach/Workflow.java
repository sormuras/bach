package com.github.sormuras.bach;

public abstract class Workflow {

  protected final Bach bach;
  protected final Project project;

  protected Workflow(Bach bach) {
    this.bach = bach;
    this.project = bach.project();
  }

  public abstract void execute();

  protected void checkpoint(Checkpoint checkpoint) {
    bach.settings().workflowSettings().listener().at(checkpoint);
  }

  public interface Checkpoint {
    Bach bach();
  }

  @FunctionalInterface
  public interface CheckpointListener {
    void at(Checkpoint checkpoint);
  }

  @FunctionalInterface
  public interface Tweak {
    Call tweak(Call call);
  }
}
