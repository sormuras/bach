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
      run.bach.toolbox.ListFilesTool;
  provides run.bach.ToolOperator with
      run.bach.toolbox.HashOperator,
      run.bach.toolbox.ImportOperator,
      run.bach.toolbox.InfoOperator,
      run.bach.toolbox.InstallOperator,
      run.bach.toolbox.LoadFileOperator,
      run.bach.toolbox.LoadHeadOperator,
      run.bach.toolbox.LoadModuleOperator,
      run.bach.toolbox.LoadModulesOperator,
      run.bach.toolbox.LoadTextOperator,
      run.bach.toolbox.ListPathsOperator,
      run.bach.toolbox.ListModulesOperator,
      run.bach.toolbox.ListToolsOperator,
      run.bach.toolbox.SignatureOperator,
      run.bach.project.BuildOperator,
      run.bach.project.CacheOperator,
      run.bach.project.CompileOperator,
      run.bach.project.CompileClassesOperator,
      run.bach.project.CompileModulesOperator,
      run.bach.project.LaunchOperator,
      run.bach.project.TestOperator;
}
