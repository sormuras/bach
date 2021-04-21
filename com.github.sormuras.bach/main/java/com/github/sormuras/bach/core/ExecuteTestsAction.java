package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.api.Action;

public class ExecuteTestsAction extends BachAction {

  public ExecuteTestsAction(Bach bach) {
    super(Action.EXECUTE_TESTS, bach);
  }

  public void execute() {}
}