package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;

public abstract class AbstractProjectWorkflow implements Workflow {

  protected final Bach bach;
  protected final Project project;

  protected AbstractProjectWorkflow(Bach bach, Project project) {
    this.bach = bach;
    this.project = project;
  }
}
