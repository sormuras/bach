package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;

public class CompileTestModulesWorkflow extends CompileWorkflow {

  public CompileTestModulesWorkflow(Bach bach) {
    super(bach, new Space("test", bach.project().testModules().set()));
  }
}
