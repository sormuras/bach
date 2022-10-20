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

  uses java.util.spi.ToolProvider;
  uses run.bach.Bach.Factory;
  uses run.bach.ExternalModulesLocator;
  uses run.bach.Project.Composer;
  uses run.bach.ToolOperator;
  uses run.bach.ToolTweak;

  provides java.util.spi.ToolProvider with
      run.bach.tool.ListFilesTool,
      run.bach.tool.TreeCreateTool,
      run.bach.tool.TreeDeleteTool,
      run.bach.tool.TreeTool;
  provides run.bach.ToolOperator with
      run.bach.tool.HashOperator,
      run.bach.tool.ImportOperator,
      run.bach.tool.InfoOperator,
      run.bach.tool.InstallOperator,
      run.bach.tool.LoadFileOperator,
      run.bach.tool.LoadHeadOperator,
      run.bach.tool.LoadModuleOperator,
      run.bach.tool.LoadModulesOperator,
      run.bach.tool.LoadTextOperator,
      run.bach.tool.ListPathsOperator,
      run.bach.tool.ListModulesOperator,
      run.bach.tool.ListToolsOperator,
      run.bach.tool.SignatureOperator,
      run.bach.project.BuildOperator,
      run.bach.project.CacheOperator,
      run.bach.project.CompileOperator,
      run.bach.project.CompileClassesOperator,
      run.bach.project.CompileModulesOperator,
      run.bach.project.LaunchOperator,
      run.bach.project.TestOperator;
}
