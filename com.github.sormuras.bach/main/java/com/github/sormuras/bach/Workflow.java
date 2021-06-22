package com.github.sormuras.bach;

public abstract class Workflow {

  protected final Bach bach;
  protected final Project project;

  protected Workflow(Bach bach, Project project) {
    this.bach = bach;
    this.project = project;
  }

  public void execute() {}
}
