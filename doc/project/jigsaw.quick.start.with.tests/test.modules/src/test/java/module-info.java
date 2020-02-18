module test.modules {
  requires com.greetings;
  requires org.astro;
  requires test.base;

  provides java.util.spi.ToolProvider with
      test.modules.TestProvider;
}
