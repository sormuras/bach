package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;

public class BuildAction extends BachAction {

  public BuildAction(Bach bach) {
    super(bach);
  }

  public void build() {
    bach().log("Build...");
    bach().compileMainCodeSpace();
    bach().compileTestCodeSpace();
    bach().executeTests();
    bach().generateDocumentation();
  }
}