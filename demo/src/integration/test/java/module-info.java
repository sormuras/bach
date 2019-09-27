// --main-class integration.Main
open /*test*/ module integration {

  // modules under test
  requires de.sormuras.bach.demo;
  requires de.sormuras.bach.demo.multi;

  // modules used for testing
  requires org.junit.jupiter;
  requires org.junit.platform.launcher;

  provides java.util.spi.ToolProvider with
      integration.Main;
}
