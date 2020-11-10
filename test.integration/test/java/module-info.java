open /*test*/ module test.integration {
  requires com.github.sormuras.bach;
  requires org.junit.jupiter;
  requires test.base;

  provides java.util.spi.ToolProvider with test.integration.PrintToolProvider;
}
