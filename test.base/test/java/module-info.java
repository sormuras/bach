open /*test*/ module test.base {
  exports test.base;

  requires transitive org.junit.jupiter;

  provides java.util.spi.ToolProvider with
      test.base.TestProvider1,
      test.base.TestProvider2;
}
