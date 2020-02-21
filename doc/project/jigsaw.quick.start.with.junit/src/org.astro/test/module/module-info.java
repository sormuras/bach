open /*test*/ module org.astro {
  exports org.astro;

  requires test.base;

  provides java.util.spi.ToolProvider with
      org.astro.TestProvider;
}
