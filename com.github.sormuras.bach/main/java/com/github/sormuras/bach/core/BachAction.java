package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.api.Action;

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