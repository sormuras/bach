package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;

public class BuildWorkflow extends BachWorkflow {

  public BuildWorkflow(Bach bach) {
    super(bach);
  }

  public void build() {
    bach().log("Build begin...");
    var workflows = bach().settings().workflows();
    workflows.newResolveWorkflow().with(bach()).resolve();
    workflows.newCompileMainCodeSpaceWorkflow().with(bach()).compile();
    workflows.newCompileTestCodeSpaceWorkflow().with(bach()).compile();
    workflows.newExecuteTestsWorkflow().with(bach()).execute();
    workflows.newGenerateDocumentationWorkflow().with(bach()).generate();
    workflows.newGenerateImageWorkflow().with(bach()).generate();
    bach().log("Build end.");
  }
}