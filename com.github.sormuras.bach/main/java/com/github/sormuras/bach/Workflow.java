package com.github.sormuras.bach;

public abstract class Workflow {

  protected final Bach bach;
  protected final Project project;

  protected Workflow(Bach bach) {
    this.bach = bach;
    this.project = bach.project();
  }

  public abstract void execute();
}
