package run.bach;

import run.duke.ToolRunner;

@FunctionalInterface
public interface Workbench extends ToolRunner {
  default Options options() {
    return Options.DEFAULTS;
  }

  default Folders folders() {
    return Folders.CURRENT_WORKING_DIRECTORY;
  }

  default Printer printer() {
    return Printer.BROKEN;
  }
}
