package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Workflow;

public class ManageExternalModulesWorkflow extends Workflow {

  public ManageExternalModulesWorkflow(Bach bach) {
    super(bach);
  }

  @Override
  public void execute() {
    bach.resolver().resolveMissingExternalModules();
  }
}
