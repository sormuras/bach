module hello {
  provides java.util.spi.ToolProvider with
      hello.Hello;
}
