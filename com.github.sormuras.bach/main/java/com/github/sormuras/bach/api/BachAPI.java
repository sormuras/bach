package com.github.sormuras.bach.api;

/** Defines the extensible API of the Java Shell Builder by combining overlay API declarations. */
public interface BachAPI
    extends JavaFormatterAPI,
        ExternalModuleAPI,
        ProjectComputerAPI,
        ProjectBuildMainSpaceAPI,
        ProjectBuildTestSpaceAPI,
        ToolProviderAPI,
        LogbookWriterAPI {

  /** Build the current project. */
  void build() throws Exception;

  /** Format source code files. */
  void format();
}
