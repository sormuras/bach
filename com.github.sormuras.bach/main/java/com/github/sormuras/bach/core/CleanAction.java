package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.internal.Paths;

public class CleanAction extends BachAction {

  public CleanAction(Bach bach) {
    super(bach);
  }

  public void clean() {
    var workspace = bach().project().folders().workspace();
    bach().say("Delete Bach's workspace: %s".formatted(workspace));
    Paths.deleteDirectories(bach().project().folders().workspace());
  }
}
