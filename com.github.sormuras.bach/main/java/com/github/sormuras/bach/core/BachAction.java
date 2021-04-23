package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;

abstract class BachAction {

  private final Bach bach;

  protected BachAction(Bach bach) {
    this.bach = bach;
  }

  public Bach bach() {
    return bach;
  }
}