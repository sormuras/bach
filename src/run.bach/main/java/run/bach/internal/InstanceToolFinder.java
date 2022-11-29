package run.bach.internal;

import java.util.List;
import java.util.Optional;
import run.bach.Tool;
import run.bach.ToolFinder;
import run.bach.ToolRunner;

public record InstanceToolFinder(String description, List<Tool> tools) implements ToolFinder {
  public InstanceToolFinder(String description, Tool... tools) {
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
