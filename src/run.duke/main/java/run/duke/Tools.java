package run.duke;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public record Tools(String description, List<Tool> list) implements Iterable<Tool>, ToolFinder {
  @Override
  public Iterator<Tool> iterator() {
    return list.iterator();
  }

  @Override
  public Optional<Tool> find(String name, ToolRunner runner) {
    for (var tool : list) if (tool.test(name)) return Optional.of(tool);
    return Optional.empty();
  }

  @Override
  public List<String> identifiers() {
    return list.stream().map(Tool::identifier).sorted().toList();
  }
}
