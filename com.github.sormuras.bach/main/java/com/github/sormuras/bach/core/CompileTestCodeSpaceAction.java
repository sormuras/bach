package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.api.Action;

public class CompileTestCodeSpaceAction extends BachAction {

  public CompileTestCodeSpaceAction(Bach bach) {
    super(Action.COMPILE_TEST, bach);
  }

  public void compile() {}
}