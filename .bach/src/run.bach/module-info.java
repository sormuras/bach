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

  provides run.bach.ToolOperator with
      run.bach.toolbox.HashTool,
      run.bach.toolbox.ImportTool,
      run.bach.toolbox.InfoTool,
      run.bach.toolbox.InstallTool,
      run.bach.toolbox.ListTool,
      run.bach.toolbox.LoadTool,
      run.bach.toolbox.SignTool,
      run.bach.project.BuildTool,
      run.bach.project.CacheTool,
      run.bach.project.CompileTool,
      run.bach.project.CompileClassesTool,
      run.bach.project.CompileModulesTool,
      run.bach.project.LaunchTool,
      run.bach.project.TestTool;
}
