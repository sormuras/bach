package com.github.sormuras.bach.trait;

import com.github.sormuras.bach.Trait;
import com.github.sormuras.bach.api.Workflow;
import com.github.sormuras.bach.api.UnsupportedWorkflowException;

public /*sealed*/ interface ActionTrait extends Trait {

  default void run(Workflow workflow) {
    bach().log("run(%s)".formatted(workflow));
    switch (workflow) {
      case BUILD -> build();
      case CLEAN -> clean();
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
    bach().factory().newBuildWorkflow(bach()).build();
  }

  default void clean() {
    bach().factory().newCleanWorkflow(bach()).clean();
  }

  default void compileMainCodeSpace() {
    bach().factory().newCompileMainCodeSpaceWorkflow(bach()).compile();
  }

  default void compileTestCodeSpace() {
    bach().factory().newCompileTestCodeSpaceWorkflow(bach()).compile();
  }

  default void executeTests() {
    bach().factory().newExecuteTestsWorkflow(bach()).execute();
  }

  default void generateDocumentation() {
    bach().factory().newGenerateDocumentationWorkflow(bach()).generate();
  }

  default void generateImage() {
    bach().factory().newGenerateImageWorkflow(bach()).generate();
  }

  default void writeLogbook() {
    bach().factory().newWriteLogbookWorkflow(bach()).write();
  }
}
