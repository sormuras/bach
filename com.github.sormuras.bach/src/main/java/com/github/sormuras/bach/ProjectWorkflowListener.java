package com.github.sormuras.bach;

public sealed interface ProjectWorkflowListener permits ProjectInfoConfigurator {
  default void onWorklowBuildBegin(Bach bach) {}

  default void onWorklowBuildEnd(Bach bach) {}

  default void onWorklowCacheBegin(Bach bach) {}

  default void onWorklowCacheEnd(Bach bach) {}

  default void onWorklowCompileBegin(Bach bach) {}

  default void onWorklowCompileEnd(Bach bach) {}
}
