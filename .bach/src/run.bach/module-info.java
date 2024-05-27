/** Defines Bach's API. */
module run.bach {
  requires jdk.compiler;
  requires transitive jdk.jfr;

  exports run.bach;
  exports run.bach.info;
  exports run.bach.workflow;

  uses java.util.spi.ToolProvider;
}
