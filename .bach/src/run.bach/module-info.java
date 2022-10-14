import java.util.spi.*;
import run.bach.*;
import run.bach.internal.tool.*;

/** Defines Bach's API. */
module run.bach {
  requires transitive java.net.http;
  requires jdk.compiler;
  requires jdk.jartool;
  requires jdk.javadoc;
  requires jdk.jdeps;
  requires jdk.jfr;
  requires jdk.jlink;
  requires jdk.jpackage;

  exports run.bach;
  exports run.bach.project;

  uses BachFactory;
  uses Locator;
  uses ToolOperator;
  uses ToolProvider;

  provides ToolOperator with
      HashOperator,
      ImportOperator,
      InfoOperator,
      InstallOperator,
      LoadFileOperator,
      LoadHeadOperator,
      LoadModuleOperator,
      LoadModulesOperator,
      LoadTextOperator,
      ListPathsOperator,
      ListModulesOperator,
      ListToolsOperator,
      SignatureOperator;
  provides ToolProvider with
      ListFilesTool,
      TreeCreateTool,
      TreeDeleteTool,
      TreeTool;
}
