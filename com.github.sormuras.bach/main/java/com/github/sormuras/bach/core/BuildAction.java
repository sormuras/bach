package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.api.Action;

public class BuildAction extends BachAction {

  public BuildAction(Bach bach) {
    super(Action.BUILD, bach);
  }

  public void build() {
    bach().log("Build...");
    bach().compileMainCodeSpace();
    bach().compileTestCodeSpace();
    bach().executeTests();
  }
}