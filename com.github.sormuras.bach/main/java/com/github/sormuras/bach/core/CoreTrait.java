package com.github.sormuras.bach.core;

public interface CoreTrait extends BachTrait {
  default void build() {
    bach().plugins().newBuildAction(bach()).build();
  }

  default void clean() {
    bach().plugins().newCleanAction(bach()).clean();
  }

  default void compileMainCodeSpace() {
    bach().plugins().newCompileMainCodeSpaceAction(bach()).compile();
  }

  default void compileTestCodeSpace() {
    bach().plugins().newCompileTestCodeSpaceAction(bach()).compile();
  }

  default void executeTests() {
    bach().plugins().newExecuteTestsAction(bach()).execute();
  }

  default void writeLogbook() {
    bach().plugins().newWriteLogbookAction(bach()).write();
  }
}
