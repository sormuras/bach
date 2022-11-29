module test.bach {
  requires run.bach;
  requires jdk.jfr;

  uses java.util.spi.ToolProvider; // in test.bach.Main

  provides java.util.spi.ToolProvider with
      test.bach.OptionsTests,
      test.bach.Main,
      test.bach.ToolboxTests,
      test.bach.ToolTests;
  provides run.bach.ToolFinder with
      test.bach.MockToolFinder;
}
