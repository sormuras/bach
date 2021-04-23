package com.github.sormuras.bach;

import com.github.sormuras.bach.core.BuildAction;
import com.github.sormuras.bach.core.CleanAction;
import com.github.sormuras.bach.core.CompileMainCodeSpaceAction;
import com.github.sormuras.bach.core.CompileTestCodeSpaceAction;
import com.github.sormuras.bach.core.ExecuteTestsAction;
import com.github.sormuras.bach.core.ProjectBuilder;
import com.github.sormuras.bach.core.WriteLogbookAction;

public class Factory {

  public Factory() {}

  public ProjectBuilder newProjectBuilder(Logbook logbook, Options options) {
    return new ProjectBuilder(logbook, options);
  }

  public BuildAction newBuildAction(Bach bach) {
    return new BuildAction(bach);
  }

  public CleanAction newCleanAction(Bach bach) {
    return new CleanAction(bach);
  }

  public CompileMainCodeSpaceAction newCompileMainCodeSpaceAction(Bach bach) {
    return new CompileMainCodeSpaceAction(bach);
  }

  public CompileTestCodeSpaceAction newCompileTestCodeSpaceAction(Bach bach) {
    return new CompileTestCodeSpaceAction(bach);
  }

  public ExecuteTestsAction newExecuteTestsAction(Bach bach) {
    return new ExecuteTestsAction(bach);
  }

  public WriteLogbookAction newWriteLogbookAction(Bach bach) {
    return new WriteLogbookAction(bach);
  }
}
