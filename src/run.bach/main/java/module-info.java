/** Defines the API of Bach. */
@run.bach.Command(name = "--list-tools", args = "duke list tools")
module run.bach {
  requires transitive java.net.http;
  requires jdk.compiler;
  requires jdk.jartool;
  requires jdk.javadoc;
  requires jdk.jdeps;
  requires jdk.jfr;
  requires jdk.jlink;
  requires jdk.jpackage;
  requires transitive run.duke;

  exports run.bach;
  exports run.bach.external;
  exports run.bach.tool;

  uses java.util.spi.ToolProvider;
  uses run.bach.Composer;

  provides java.util.spi.ToolProvider with
      run.bach.tool.BuildTool,
      run.bach.tool.CacheTool,
      run.bach.tool.CleanTool,
      run.bach.tool.CompileTool,
      run.bach.tool.CompileClassesTool,
      run.bach.tool.CompileModulesTool,
      run.bach.tool.ImportTool,
      run.bach.tool.InfoTool,
      run.bach.tool.InstallTool,
      run.bach.tool.LaunchTool,
      run.bach.tool.LoadTool,
      run.bach.tool.TestTool;
}
