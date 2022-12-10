module test.duke {
  requires run.duke;
  requires jdk.jfr;

  uses java.util.spi.ToolProvider; // in test.duke.Main

  provides java.util.spi.ToolProvider with
      test.duke.Main,
      test.duke.ToolFindersTests,
      test.duke.ToolTests;
  provides run.duke.ToolFinder with
      test.duke.MockToolFinder;
}
