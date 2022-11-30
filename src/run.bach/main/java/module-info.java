/** Defines the API of Bach. */
module run.bach {
  requires jdk.compiler;
  requires transitive run.duke;

  exports run.bach;
  exports run.bach.tool;

  uses java.util.spi.ToolProvider;
  uses run.bach.ProjectFactory;
  uses run.duke.ToolFinder;

  provides run.duke.ToolFinder with
      run.bach.ProjectToolFinder;
}
