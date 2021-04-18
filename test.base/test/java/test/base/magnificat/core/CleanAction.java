package test.base.magnificat.core;

import test.base.magnificat.Bach;
import test.base.magnificat.api.Action;

public class CleanAction extends BachAction {

  public CleanAction(Bach bach) {
    super(Action.CLEAN, bach);
  }

  public void clean() {}
}
