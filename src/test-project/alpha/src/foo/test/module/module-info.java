/*test*/ module foo {
  provides java.util.spi.ToolProvider with
      foo.Tester;
}
