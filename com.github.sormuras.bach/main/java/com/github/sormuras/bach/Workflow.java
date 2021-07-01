package com.github.sormuras.bach;

public abstract class Workflow {

  protected final Bach bach;
  protected final Settings settings;
  protected final Project project;

  protected Workflow(Bach bach) {
    this.bach = bach;
    this.settings = bach.settings();
    this.project = bach.project();
  }

  public abstract void execute();

  public Call.Tree generateCallTree() {
    return Call.tree(getClass().getSimpleName());
  }
}
