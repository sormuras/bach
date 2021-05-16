package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.internal.Paths;
import java.nio.file.Files;

public class CleanWorkflow extends BachWorkflow {

  public CleanWorkflow(Bach bach) {
    super(bach);
  }

  public void clean() {
    cleanWorkspace();
  }

  protected void cleanWorkspace() {
    var workspace = bach().project().folders().workspace();
    if (Files.notExists(workspace)) {
      bach().log("No workspace directory found, nothing to do here.");
      return;
    }
    bach().say("Delete Bach's workspace: %s".formatted(workspace));
    Paths.deleteDirectories(workspace);
  }
}
