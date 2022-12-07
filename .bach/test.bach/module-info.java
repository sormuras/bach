module test.bach {
  requires run.bach;
  requires run.duke;
  requires jdk.jfr;

  uses java.util.spi.ToolProvider; // in test.bach.Main

  provides java.util.spi.ToolProvider with
      test.bach.CommandModeTests,
      test.bach.OptionsTests,
      test.bach.Main,
      test.bach.ToolFindersTests,
      test.bach.ToolTests;
  provides run.duke.ToolFinder with
      test.bach.MockToolFinder;
}
