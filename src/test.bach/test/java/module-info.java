open /*test*/ module test.bach {
  requires run.bach;

  provides java.util.spi.ToolProvider with
      test.bach.BachTests;
}
