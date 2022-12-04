package run.duke.tool;

import java.util.List;
import run.duke.EnumToolFinder;

public record DukeToolFinder(String description, List<DukeToolInfo> constants)
    implements EnumToolFinder<DukeToolInfo> {
  public DukeToolFinder() {
    this("Duke's Default Tools", List.of(DukeToolInfo.values()));
  }
}
