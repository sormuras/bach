open /*for testing*/ module test.bach {
  requires run.bach; // module under test
  requires static org.junit.platform.console; // for running tests
  requires org.junit.jupiter; // for writing tests

  provides java.util.spi.ToolProvider with
      test.bach.Tests;
}
