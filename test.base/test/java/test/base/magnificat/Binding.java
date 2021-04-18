package test.base.magnificat;

import test.base.magnificat.core.BuildAction;
import test.base.magnificat.core.CleanAction;
import test.base.magnificat.core.CompileMainCodeSpaceAction;
import test.base.magnificat.core.CompileTestCodeSpaceAction;
import test.base.magnificat.core.ExecuteTestsAction;
import test.base.magnificat.core.ProjectFactory;
import test.base.magnificat.core.WriteLogbookAction;

public class Binding {

  protected Binding() {}

  protected ProjectFactory newProjectFactory(Configuration configuration) {
    return new ProjectFactory(configuration);
  }

  protected BuildAction newBuildAction(Bach bach) {
    return new BuildAction(bach);
  }

  protected CleanAction newCleanAction(Bach bach) {
    return new CleanAction(bach);
  }

  protected CompileMainCodeSpaceAction newCompileMainCodeSpaceAction(Bach bach) {
    return new CompileMainCodeSpaceAction(bach);
  }

  protected CompileTestCodeSpaceAction newCompileTestCodeSpaceAction(Bach bach) {
    return new CompileTestCodeSpaceAction(bach);
  }

  protected ExecuteTestsAction newExecuteTestsAction(Bach bach) {
    return new ExecuteTestsAction(bach);
  }

  protected WriteLogbookAction newWriteLogbookAction(Bach bach) {
    return new WriteLogbookAction(bach);
  }
}
