package run.duke.tool;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import run.duke.Tool;
import run.duke.ToolFinder;
import run.duke.ToolRunner;

public record DukeToolFinder() implements ToolFinder {
  @Override
  public String description() {
    return "Duke's built-in tools";
  }

  @Override
  public List<String> identifiers() {
    return Stream.of(DukeToolInfo.values()).map(DukeToolInfo::identifier).toList();
  }

  @Override
  public Optional<Tool> find(String string, ToolRunner runner) {
    var values = DukeToolInfo.values();
    for (var info : values) if (info.test(string)) return Optional.of(info.tool(runner));
    return Optional.empty();
  }
}
