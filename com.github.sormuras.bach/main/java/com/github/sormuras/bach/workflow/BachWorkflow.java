package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;

/**
 * Base workflow.
 */
abstract class BachWorkflow {

  private final Bach bach;

  BachWorkflow(Bach bach) {
    this.bach = bach;
  }

  public Bach bach() {
    return bach;
  }
}