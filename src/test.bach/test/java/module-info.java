module test.bach {
  requires run.bach;
  requires jdk.httpserver;
  requires jdk.jfr;

  uses java.util.spi.ToolProvider; // in test.bach.Main

  provides java.util.spi.ToolProvider with
      test.bach.BrowserTests,
      test.bach.CommandModeTests,
      test.bach.OptionsTests,
      test.bach.Main;
}
