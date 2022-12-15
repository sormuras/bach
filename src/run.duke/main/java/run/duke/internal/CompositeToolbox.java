package run.duke.internal;

import java.util.List;
import run.duke.Tool;
import run.duke.Toolbox;

public record CompositeToolbox(List<Toolbox> toolboxes) implements Toolbox {
  @Override
  public List<Tool> tools() {
    return toolboxes.stream().flatMap(toolbox -> toolbox.tools().stream()).toList();
  }
}
