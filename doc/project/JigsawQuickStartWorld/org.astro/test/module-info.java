open /*test*/ module org.astro {
  exports org.astro;

  provides java.util.spi.ToolProvider with
      org.astro.TestProvider;
}
