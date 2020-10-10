open /*test*/ module tests {
  requires com.github.sormuras.bach;
  requires org.junit.jupiter;

  provides java.util.spi.ToolProvider with
      tests.util.Print;
}
