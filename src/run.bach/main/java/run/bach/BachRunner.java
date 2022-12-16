package run.bach;

import run.duke.ToolRunner;

@FunctionalInterface
public interface BachRunner extends ToolRunner {
  default Project project() {
    return workbench().workpiece(Project.class);
  }

  default Options options() {
    return workbench().workpiece(Options.class);
  }

  default Folders folders() {
    return workbench().workpiece(Folders.class);
  }

  default Printer printer() {
    return workbench().workpiece(Printer.class);
  }

  default Toolkit toolkit() {
    return workbench().workpiece(Toolkit.class);
  }
}
