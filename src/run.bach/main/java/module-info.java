/** Defines the API of Bach. */
@run.bach.Command(name = "--list-tools", args = "duke list tools")
module run.bach {
  requires jdk.compiler;
  requires jdk.jartool;
  requires jdk.javadoc;
  requires jdk.jdeps;
  requires jdk.jlink;
  requires jdk.jpackage;
  requires transitive run.duke;

  exports run.bach;
  exports run.bach.tool;

  uses java.util.spi.ToolProvider;
  uses run.bach.Composer;
  uses run.duke.ToolFinder;
}
