package run.duke;

import java.util.List;
import java.util.Optional;

public record DefaultToolFinder(String description, List<Tool> tools) implements ToolFinder {
  public DefaultToolFinder(String description, Tool... tools) {
    this(description, List.of(tools));
  }

  @Override
  public Optional<Tool> find(String name, ToolRunner runner) {
    for (var tool : tools) if (tool.test(name)) return Optional.of(tool);
    return Optional.empty();
  }

  @Override
  public List<String> identifiers() {
    return tools.stream().map(Tool::identifier).toList();
  }
}
