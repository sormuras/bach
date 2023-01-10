package run.bach;

import run.duke.ToolRunner;

public interface ProjectRunner extends ToolRunner {
  default Browser browser() {
    return getConstant(Browser.class);
  }

  default Options options() {
    return getConstant(Options.class);
  }

  default Folders folders() {
    return getConstant(Folders.class);
  }

  default Printer printer() {
    return getConstant(Printer.class);
  }

  default Project project() {
    return getConstant(Project.class);
  }

  default Toolkit toolkit() {
    return getConstant(Toolkit.class);
  }
}
