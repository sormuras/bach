package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.api.Action;

public class CleanAction extends BachAction {

  public CleanAction(Bach bach) {
    super(Action.CLEAN, bach);
  }

  public void clean() {}
}