package test.base.magnificat.core;

import test.base.magnificat.Bach;
import test.base.magnificat.api.Action;

public class BuildAction extends BachAction {

  public BuildAction(Bach bach) {
    super(Action.BUILD, bach);
  }

  public void build() {
    bach().compileMainCodeSpace();
    bach().compileTestCodeSpace();
    bach().executeTests();
  }
}
