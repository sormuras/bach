module test.modules {
  requires com.greetings;
  requires org.astro;

  provides java.util.spi.ToolProvider with
      test.modules.TestProvider;
}
