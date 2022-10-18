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
  exports run.bach.project.workflow;

  uses java.util.spi.ToolProvider;
  uses run.bach.BachFactory;
  uses run.bach.Locator;
  uses run.bach.ToolOperator;
  uses run.bach.project.ProjectComposer;

  provides java.util.spi.ToolProvider with
      run.bach.internal.tool.ListFilesTool,
      run.bach.internal.tool.TreeCreateTool,
      run.bach.internal.tool.TreeDeleteTool,
      run.bach.internal.tool.TreeTool;
  provides run.bach.ToolOperator with
      run.bach.internal.tool.HashOperator,
      run.bach.internal.tool.ImportOperator,
      run.bach.internal.tool.InfoOperator,
      run.bach.internal.tool.InstallOperator,
      run.bach.internal.tool.LoadFileOperator,
      run.bach.internal.tool.LoadHeadOperator,
      run.bach.internal.tool.LoadModuleOperator,
      run.bach.internal.tool.LoadModulesOperator,
      run.bach.internal.tool.LoadTextOperator,
      run.bach.internal.tool.ListPathsOperator,
      run.bach.internal.tool.ListModulesOperator,
      run.bach.internal.tool.ListToolsOperator,
      run.bach.internal.tool.SignatureOperator,
      run.bach.project.workflow.Build,
      run.bach.project.workflow.Cache,
      run.bach.project.workflow.Compile,
      run.bach.project.workflow.CompileClasses,
      run.bach.project.workflow.CompileModules,
      run.bach.project.workflow.Launch,
      run.bach.project.workflow.Test;
}
