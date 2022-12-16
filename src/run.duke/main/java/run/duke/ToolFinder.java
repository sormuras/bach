package run.duke;

import java.util.Optional;

@FunctionalInterface
public interface ToolFinder {
  Workbench workbench();

  default Optional<Tool> find(String tool) {
    return workbench().toolbox().find(tool);
  }
}
