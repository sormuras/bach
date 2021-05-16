package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;

public class BuildWorkflow extends BachWorkflow {

  public BuildWorkflow(Bach bach) {
    super(bach);
  }

  public void build() {
    bach().log("Build begin...");
    bach().resolve();
    bach().compileMainCodeSpace();
    bach().compileTestCodeSpace();
    bach().executeTests();
    bach().generateDocumentation();
    bach().generateImage();
    bach().log("Build end.");
  }
}