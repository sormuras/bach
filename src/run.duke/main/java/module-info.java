/** Defines the foundational APIs of Duke, the Tool Finder and Runner SPI. */
module run.duke {
  requires jdk.compiler;
  requires jdk.jartool;
  requires jdk.javadoc;
  requires jdk.jdeps;
  requires jdk.jlink;
  requires jdk.jpackage;

  exports run.duke;

  uses java.util.spi.ToolProvider;
  uses run.duke.ToolFinder;

  provides java.util.spi.ToolProvider with
      run.duke.base.CheckJavaReleaseTool,
      run.duke.base.CheckJavaVersionTool,
      run.duke.base.TreeTool;
  provides run.duke.ToolFinder with
      run.duke.tool.DukeToolFinder;
}
