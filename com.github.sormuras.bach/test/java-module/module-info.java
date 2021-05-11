@com.github.sormuras.bach.api.ProjectInfo
open /*test*/ module com.github.sormuras.bach {
  requires org.junit.jupiter;
  requires test.base;

  exports com.github.sormuras.bach;
  exports com.github.sormuras.bach.api;
  exports com.github.sormuras.bach.api.external;
  exports com.github.sormuras.bach.core;
  exports com.github.sormuras.bach.tool;

  requires transitive java.net.http;
  requires jdk.compiler;
  requires jdk.crypto.ec; // https://stackoverflow.com/questions/55439599
  requires jdk.jartool;
  requires jdk.javadoc;
  requires jdk.jdeps;
  requires jdk.jlink;

  uses com.github.sormuras.bach.Factory;
  uses java.util.spi.ToolProvider;

  provides java.util.spi.ToolProvider with
      com.github.sormuras.bach.internal.BachToolProvider;
}
