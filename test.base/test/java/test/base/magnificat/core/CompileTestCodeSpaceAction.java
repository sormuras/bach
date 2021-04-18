package test.base.magnificat.core;

import test.base.magnificat.Bach;
import test.base.magnificat.api.Action;

public class CompileTestCodeSpaceAction extends BachAction {

  public CompileTestCodeSpaceAction(Bach bach) {
    super(Action.COMPILE_TEST, bach);
  }

  public void compile() {}
}
