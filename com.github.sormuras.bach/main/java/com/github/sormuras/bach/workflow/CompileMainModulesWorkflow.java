package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;

public class CompileMainModulesWorkflow extends CompileWorkflow {

  public CompileMainModulesWorkflow(Bach bach) {
    super(bach, new Space("main", bach.project().mainModules().set()));
  }
}
