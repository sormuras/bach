/** Defines Bach's API. */
module run.bach {
  requires jdk.compiler;
  requires jdk.jartool;
  requires jdk.javadoc;
  requires jdk.jdeps;
  requires jdk.jlink;
  requires jdk.jpackage;

  exports run.bach;

  uses java.util.spi.ToolProvider;
  uses run.bach.ToolFinder;

  provides java.util.spi.ToolProvider with
      run.bach.internal.CheckJavaReleaseTool,
      run.bach.internal.CheckJavaVersionTool;
  provides run.bach.ToolFinder with
      run.bach.internal.InternalToolFinder;
}
