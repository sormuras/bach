package run.duke.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import run.duke.Tool;
import run.duke.ToolFinder;
import run.duke.ToolRunner;

public record CollectionToolFinder(String description, Collection<Tool> tools)
    implements Iterable<Tool>, ToolFinder {
  public CollectionToolFinder {
    if (description == null) throw new IllegalArgumentException("description must not be null");
    if (description.isBlank()) throw new IllegalArgumentException("description must not be blank");
    if (tools == null) throw new IllegalArgumentException("tool collection must not be null");
  }

  @Override
  public Iterator<Tool> iterator() {
    return tools.iterator();
  }

  @Override
  public Optional<Tool> find(String name, ToolRunner runner) {
    for (var tool : tools) if (tool.test(name)) return Optional.of(tool);
    return Optional.empty();
  }

  @Override
  public List<String> identifiers() {
    return tools.stream().map(Tool::identifier).sorted().toList();
  }
}
