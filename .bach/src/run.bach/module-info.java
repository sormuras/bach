/** Defines Bach's API. */
module run.bach {
  requires jdk.compiler;
  requires jdk.jfr;

  exports run.bach;
  exports run.bach.workflow;

  uses java.util.spi.ToolProvider;
}
