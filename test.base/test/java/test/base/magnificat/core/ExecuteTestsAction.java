package test.base.magnificat.core;

import test.base.magnificat.Bach;
import test.base.magnificat.api.Action;

public class ExecuteTestsAction extends BachAction {

  public ExecuteTestsAction(Bach bach) {
    super(Action.EXECUTE_TESTS, bach);
  }

  public void execute() {}
}
