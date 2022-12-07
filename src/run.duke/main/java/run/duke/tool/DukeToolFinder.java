package run.duke.tool;

import java.util.List;

import run.duke.ToolFinder;
import run.duke.ToolRunner;

public record DukeToolFinder(String description, List<DukeToolInfo> constants)
    implements ToolFinder.EnumToolFinder<DukeToolInfo, ToolRunner> {
  public DukeToolFinder() {
    this("Duke's Default Tools", List.of(DukeToolInfo.values()));
  }
}
