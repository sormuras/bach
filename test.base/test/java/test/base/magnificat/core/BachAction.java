package test.base.magnificat.core;

import test.base.magnificat.Bach;
import test.base.magnificat.api.Action;

abstract class BachAction {

  private final Action action;
  private final Bach bach;

  protected BachAction(Action action, Bach bach) {
    this.action = action;
    this.bach = bach;
  }

  public Action action() {
    return action;
  }

  public Bach bach() {
    return bach;
  }
}
