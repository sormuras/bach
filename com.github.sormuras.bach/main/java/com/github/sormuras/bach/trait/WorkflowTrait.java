package com.github.sormuras.bach.trait;

import com.github.sormuras.bach.Trait;
import com.github.sormuras.bach.api.Workflow;
import com.github.sormuras.bach.api.UnsupportedWorkflowException;

public /*sealed*/ interface WorkflowTrait extends Trait {

  default void run(Workflow workflow) {
    bach().log("run(%s)".formatted(workflow));
    switch (workflow) {
      case BUILD -> build();
      case CLEAN -> clean();
      case RESOLVE -> resolve();
      case COMPILE_MAIN -> compileMainCodeSpace();
      case COMPILE_TEST -> compileTestCodeSpace();
      case EXECUTE_TESTS -> executeTests();
      case GENERATE_DOCUMENTATION -> generateDocumentation();
      case GENERATE_IMAGE -> generateImage();
      case WRITE_LOGBOOK -> writeLogbook();
      default -> throw new UnsupportedWorkflowException(workflow.toString());
    }
  }

  default void build() {
    bach().configuration().factory().newBuildWorkflow(bach()).build();
  }

  default void clean() {
    bach().configuration().factory().newCleanWorkflow(bach()).clean();
  }

  default void resolve() {
    bach().configuration().factory().newResolveWorkflow(bach()).resolve();
  }

  default void compileMainCodeSpace() {
    bach().configuration().factory().newCompileMainCodeSpaceWorkflow(bach()).compile();
  }

  default void compileTestCodeSpace() {
    bach().configuration().factory().newCompileTestCodeSpaceWorkflow(bach()).compile();
  }

  default void executeTests() {
    bach().configuration().factory().newExecuteTestsWorkflow(bach()).execute();
  }

  default void generateDocumentation() {
    bach().configuration().factory().newGenerateDocumentationWorkflow(bach()).generate();
  }

  default void generateImage() {
    bach().configuration().factory().newGenerateImageWorkflow(bach()).generate();
  }

  default void writeLogbook() {
    bach().configuration().factory().newWriteLogbookWorkflow(bach()).write();
  }
}
