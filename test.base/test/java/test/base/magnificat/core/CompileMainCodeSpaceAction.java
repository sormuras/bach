package test.base.magnificat.core;

import test.base.magnificat.Bach;
import test.base.magnificat.api.Action;

public class CompileMainCodeSpaceAction extends BachAction {

  public CompileMainCodeSpaceAction(Bach bach) {
    super(Action.COMPILE_MAIN, bach);
  }

  public void compile() {}
}
