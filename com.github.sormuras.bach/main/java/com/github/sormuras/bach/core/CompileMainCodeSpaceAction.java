package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.api.Action;

public class CompileMainCodeSpaceAction extends BachAction {

  public CompileMainCodeSpaceAction(Bach bach) {
    super(Action.COMPILE_MAIN, bach);
  }

  public void compile() {}
}