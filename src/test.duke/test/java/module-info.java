import test.duke.ToolFinderTests;

module test.duke {
  requires run.duke;
  requires jdk.jfr;

  uses java.util.spi.ToolProvider; // in test.duke.Main

  provides java.util.spi.ToolProvider with
      test.duke.DukeTests,
      test.duke.Main,
      ToolFinderTests,
      test.duke.ToolCallTests,
      test.duke.ToolCallsTests,
      test.duke.ToolOperatorTests,
      test.duke.ToolTests;
}
