package com.github.sormuras.bach.core;

public /*sealed*/ interface CoreTrait extends BachTrait {
  default void build() {
    bach().factory().newBuildAction(bach()).build();
  }

  default void clean() {
    bach().factory().newCleanAction(bach()).clean();
  }

  default void compileMainCodeSpace() {
    bach().factory().newCompileMainCodeSpaceAction(bach()).compile();
  }

  default void compileTestCodeSpace() {
    bach().factory().newCompileTestCodeSpaceAction(bach()).compile();
  }

  default void executeTests() {
    bach().factory().newExecuteTestsAction(bach()).execute();
  }

  default void writeLogbook() {
    bach().factory().newWriteLogbookAction(bach()).write();
  }
}
