open /*test*/ module test.modules {
  requires com.github.sormuras.bach;
  requires org.junit.jupiter;
  requires test.base;

  provides java.util.spi.ToolProvider with test.integration.PrintToolProvider;
}
