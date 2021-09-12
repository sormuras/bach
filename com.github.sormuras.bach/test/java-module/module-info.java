open /*test*/ module com.github.sormuras.bach {
  requires org.junit.jupiter;
  requires test.base;

  exports com.github.sormuras.bach;
  exports com.github.sormuras.bach.command;
  exports com.github.sormuras.bach.conventional;
  exports com.github.sormuras.bach.customizable;
  exports com.github.sormuras.bach.external;
  exports com.github.sormuras.bach.project;

  requires java.base;
  requires jdk.compiler;
  requires jdk.jartool;
  requires jdk.javadoc;
  requires jdk.jdeps;
  requires jdk.jfr;
  requires jdk.jlink;

  uses java.util.spi.ToolProvider;

  provides java.util.spi.ToolProvider with
      com.github.sormuras.bach.Main;
}
