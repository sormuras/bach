module test.duke {
  requires run.duke;
  requires jdk.jfr;

  uses java.util.spi.ToolProvider; // in test.duke.Main

  provides java.util.spi.ToolProvider with
      test.duke.Main,
      test.duke.ToolboxTests,
      test.duke.ToolCallTests,
      test.duke.ToolCallsTests,
      test.duke.ToolingTests,
      test.duke.ToolOperatorTests,
      test.duke.ToolTests,
      test.duke.WorkbenchTests;
}
