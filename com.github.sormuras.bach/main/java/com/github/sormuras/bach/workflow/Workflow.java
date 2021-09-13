package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;

public abstract class Workflow implements Runnable {

  protected final Bach bach;
  protected final Project project;

  protected Workflow(Bach bach, Project project) {
    this.bach = bach;
    this.project = project;
  }
}
