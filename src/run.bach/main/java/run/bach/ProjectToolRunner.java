package run.bach;

import run.duke.ToolFinders;
import run.duke.ToolRunner;

@FunctionalInterface
public interface ProjectToolRunner extends ToolRunner {
  @Override
  default ToolFinders toolFinders() {
    return toolbox().finders();
  }

  default Options options() {
    return Options.DEFAULTS;
  }

  default Folders folders() {
    return Folders.CURRENT_WORKING_DIRECTORY;
  }

  default Printer printer() {
    return Printer.BROKEN;
  }

  default Project project() {
    return Project.UNNAMED;
  }

  default Toolbox toolbox() {
    return Toolbox.EMPTY;
  }
}
