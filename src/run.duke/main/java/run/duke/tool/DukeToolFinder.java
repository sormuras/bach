package run.duke.tool;

import java.util.List;
import run.duke.EnumToolFinder;
import run.duke.ToolRunner;

public record DukeToolFinder(String description, List<DukeToolInfo> constants)
    implements EnumToolFinder<DukeToolInfo, ToolRunner> {
  public DukeToolFinder() {
    this("Duke's Default Tools", List.of(DukeToolInfo.values()));
  }
}
