/** Defines Bach's API. */
module run.bach {
  requires jdk.compiler;
  requires jdk.jfr;

  exports run.bach;
  exports run.bach.workflow;
  exports run.external;

  uses java.util.spi.ToolProvider;
}
