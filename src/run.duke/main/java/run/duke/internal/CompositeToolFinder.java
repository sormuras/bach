package run.duke.internal;

import java.util.List;
import run.duke.Tool;
import run.duke.ToolFinder;

public record CompositeToolFinder(List<ToolFinder> toolboxes) implements ToolFinder {
  @Override
  public List<Tool> tools() {
    return toolboxes.stream().flatMap(toolbox -> toolbox.tools().stream()).toList();
  }
}
