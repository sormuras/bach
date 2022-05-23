open /*test*/ module test.base {
  exports test.base;
  exports test.base.resource;

  requires java.base;
  requires transitive jdk.httpserver;
  requires jdk.xml.dom; // #217
  requires transitive org.junit.jupiter;
  requires transitive org.junit.platform.engine;
  requires transitive org.junit.platform.launcher;

  uses java.util.spi.ToolProvider;

  provides java.util.spi.ToolProvider with
      test.base.TestProvider1,
      test.base.TestProvider2;

  provides org.junit.platform.launcher.TestExecutionListener with
      test.base.ContainerFeed;
}
