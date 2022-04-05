/** Defines an example library API. */
module org.example.lib {
  exports org.example.lib;

  provides java.util.spi.ToolProvider with
      org.example.lib.internal.EchoToolProvider;
}
